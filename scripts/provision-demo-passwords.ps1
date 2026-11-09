$ErrorActionPreference = 'Stop'

$required = @('POSTGRES_DB', 'POSTGRES_USER')
$envValues = @{}

Get-Content .\.env | ForEach-Object {
    if ($_ -match '^\s*(POSTGRES_DB|POSTGRES_USER)\s*=\s*(.+?)\s*$') {
        $envValues[$matches[1]] = $matches[2].Trim('"').Trim("'")
    }
}

foreach ($name in $required) {
    if (-not $envValues[$name]) { throw "Missing $name in .env" }
}

if ($envValues['POSTGRES_DB'] -ne 'room_reservation') {
    throw 'Safety check failed: expected POSTGRES_DB=room_reservation.'
}

$dbName = $envValues['POSTGRES_DB']
$dbUser = $envValues['POSTGRES_USER']

'CREATE EXTENSION IF NOT EXISTS pgcrypto;' |
    docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U $dbUser -d $dbName
if ($LASTEXITCODE -ne 0) { throw 'pgcrypto initialization failed.' }

foreach ($username in @('student1', 'approver1', 'admin')) {
    $secret = Read-Host "Create password for $username (12-72 UTF-8 bytes)" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    $password = $null
    try {
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        $bytes = [Text.Encoding]::UTF8.GetBytes($password)
        if ($bytes.Length -lt 12 -or $bytes.Length -gt 72) {
            throw "Password for $username must be 12-72 UTF-8 bytes."
        }
        $hex = [BitConverter]::ToString($bytes).Replace('-', '')
        $sql = "UPDATE project.users SET password_hash = crypt(convert_from(decode('$hex', 'hex'), 'UTF8'), gen_salt('bf', 12)) WHERE username = '$username';"
        $sql | docker compose exec -T db psql -X -v ON_ERROR_STOP=1 -U $dbUser -d $dbName
        if ($LASTEXITCODE -ne 0) { throw "Password update failed for $username." }
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $password = $null
    }
}

'Provisioning complete. Checking password flags (hashes are not printed):'
docker compose exec db psql -U $dbUser -d $dbName -c "SELECT username, role, enabled, (password_hash IS NOT NULL) AS has_password FROM project.users ORDER BY user_id;"
