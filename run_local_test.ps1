$env:SPRING_DATASOURCE_URL='jdbc:postgresql://ep-proud-cherry-aok96g3v-pooler.c-2.ap-southeast-1.aws.neon.tech/Uber-bookaro?sslmode=require&channel_binding=require'
$env:SPRING_DATASOURCE_USERNAME='neondb_owner'
$env:SPRING_DATASOURCE_PASSWORD=$env:SPRING_DATASOURCE_PASSWORD
$env:JWT_SECRET_KEY='dev-secret-key-abcdefghijklmnopqrstuvwxyz1234'
$env:APP_SECURITY_JWTSECRET='dev-super-secret-012345678901234567890'
$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:3000'
$env:CLOUDINARY_CLOUD_NAME='dev-cloud'
$env:CLOUDINARY_API_KEY='dev-key'
$env:CLOUDINARY_API_SECRET='dev-secret'
$env:APP_SECURITY_ALLOWED_ORIGINS='http://localhost:3000'
$env:SPRING_MAIL_HOST='localhost'
$env:SPRING_PROFILES_ACTIVE='local'
Write-Host "Starting backend jar with provided DB using Start-Process (local profile)..."
$args = @('-jar','target\uberApp-0.0.1-SNAPSHOT.jar')
Start-Process -FilePath 'java' -ArgumentList $args -NoNewWindow -Wait
