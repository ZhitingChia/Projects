# AWS deployment guide

The delivered application runs locally. These instructions describe a future single-instance deployment; no AWS resources have been created or validated. Choose region, domain and budget before provisioning. Keep the frontend and API on one HTTPS origin to preserve session/CSRF behavior.

## Target architecture

```text
Browser → HTTPS reverse proxy on EC2 → application container :8080
                                      ├─ private RDS PostgreSQL 17
                                      ├─ private S3 artwork bucket
                                      └─ SES SMTP transactional email
```

Use an EC2 instance role, encrypted RDS storage/backups, private database subnets, and an S3 bucket with Block Public Access enabled. Permit database port 5432 only from the application's security group. Expose only HTTPS and the HTTP certificate-validation/redirect endpoint; prefer Systems Manager Session Manager over public SSH. Do not publish PostgreSQL, Mailpit, Docker, or the internal app port publicly.

## Prepare resources and configuration

1. Create RDS with PostgreSQL 17 and a dedicated least-privilege application database user. Flyway needs schema migration privileges; for stricter production separation run migrations using a dedicated deployment identity before starting the application.
2. Create a private S3 bucket, enforce TLS, default encryption and versioning. Use a dedicated bucket for application artwork: the janitor reconciles generated UUID PNG keys in that bucket. No public object URLs are required; the app streams bytes after checking ownership.
3. Give the EC2 role only `s3:ListBucket` on that bucket and `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` on its objects. Add KMS permissions only if using a customer-managed key. Do not embed AWS access keys in images or frontend code.
4. Verify a sending domain in SES in the selected region; configure DKIM and appropriate SPF/DMARC DNS records. Request SES production access before emailing arbitrary users. SES sandbox recipient restrictions otherwise prevent normal public registration. Create regional SMTP credentials; they are distinct from general AWS access keys.
5. Store database and SMTP credentials and the Google Books key in Secrets Manager. Allow the EC2 role to read only those secrets. Deployment automation should fetch them into a root-readable environment file outside the repository with mode `0600`; never print its contents or bake it into an image.
6. Set up DNS and certificates for a real domain. Configure the reverse proxy to redirect HTTP to HTTPS, preserve Host, and forward only sanitized trusted headers. Set an upload request limit slightly above 6 MiB and a request timeout long enough for bounded metadata calls.

Relevant AWS references: [S3 public access controls](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html), [SES production access](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html), [SES identities](https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html), [EC2 IAM roles](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html).

## Application environment

```dotenv
SERVER_ADDRESS=0.0.0.0
PORT=8080
APP_BASE_URL=https://library.example.com
COOKIE_SECURE=true
DATABASE_URL=jdbc:postgresql://YOUR_RDS_HOST:5432/lifetracker?sslmode=verify-full&sslrootcert=/app/certs/rds-ca.pem
DATABASE_USER=lifetracker
DATABASE_PASSWORD=FROM_SECRETS_MANAGER
STORAGE_TYPE=s3
S3_BUCKET=YOUR_PRIVATE_BUCKET
AWS_REGION=ap-southeast-1
SMTP_HOST=email-smtp.ap-southeast-1.amazonaws.com
SMTP_PORT=587
SMTP_AUTH=true
SMTP_TLS=true
SMTP_USER=FROM_SECRETS_MANAGER
SMTP_PASSWORD=FROM_SECRETS_MANAGER
MAIL_FROM=library@YOUR_VERIFIED_DOMAIN
GOOGLE_BOOKS_KEY=FROM_SECRETS_MANAGER
```

Mount the current RDS CA bundle read-only at the configured certificate path. Obtain and verify the CA bundle from AWS's official RDS documentation, not an arbitrary download. Build the release image in CI, scan it, tag it immutably, push to private ECR, and run it as the image's non-root user. Supply environment variables through the protected environment file and an instance role through the normal AWS SDK credentials chain. Do not deploy the local Compose database/Mailpit services.

Forwarded headers must only be trusted from your reverse proxy. The default backend does not trust forwarded client addresses. Set up Tomcat's trusted proxy configuration if accurate client-IP throttling is needed behind the proxy; do not accept arbitrary `X-Forwarded-For` from the internet. Add proxy-level login and registration throttling before public launch. The application limiter is intentionally in-memory for a single instance; scaling requires a shared limit mechanism.

## Release and recovery

- Take database and artwork backups before migration. On a fresh deployment Flyway runs at startup and Hibernate validates the schema.
- Start one application instance; check `/api/v1/health`, logs, TLS, secure cookies, CSRF rejection and verification/reset email delivery.
- Use two accounts to confirm private library and artwork isolation. Verify S3 upload, retrieval, replacement and deletion cleanup. Verify real provider outage/manual entry behavior.
- Configure monitoring for health, error rates, database capacity, failed email delivery, and cleanup backlog. Do not collect request bodies or description text. SES delivery/bounce/complaint handling needs an operational process before broad public registration.
- Keep RDS automated backups and test restoration. S3 versioning can retain deleted artwork; define a retention policy and document it for user privacy. Restore matching database/object snapshots together.
- Roll back an application image only when its schema remains compatible. Never blindly reverse a destructive migration. Recover database and matching artwork from tested backups if necessary.

This baseline is not highly available: EC2 failure interrupts service. JDBC sessions support later instance scaling, but scheduling, rate limiting, backup coordination and reverse-proxy infrastructure must be revisited before multiple replicas. Account export/deletion and MFA are deferred; assess privacy requirements before operating a public service.
