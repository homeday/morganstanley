$url = "http://example.com"
$headers = @{
    "HeaderName1" = "HeaderValue1"
    "HeaderName2" = "HeaderValue2"
}
$response = Invoke-WebRequest -Uri $url -UseDefaultCredentials -Headers $headers
# Get the HTTP status code
$statusCode = $response.StatusCode
Write-Output "HTTP Status Code: $statusCode"

# Function to get a nested property from a JSON object
function Get-NestedProperty {
    param (
        [Parameter(Mandatory=$true)]
        [psobject]$JsonObject,
        [Parameter(Mandatory=$true)]
        [string]$Path
    )
    $properties = $Path.Trim(':').Split('.')
    $current = $JsonObject
    foreach ($property in $properties) {
        if ($null -ne $current) {
            $current = $current.$property
        } else {
            return $null
        }
    }
    return $current
}

# Check if the content is JSON and parse it
if ($response.ContentType -eq "application/json") {
    $jsonContent = $response.Content | ConvertFrom-Json
    # Access a specific field from the JSON using a path similar to jq
    $path = ":a.b"  # Example path
    $specificField = Get-NestedProperty -JsonObject $jsonContent -Path $path
    Write-Output "Specific Field: $specificField"
} else {
    Write-Output "Content is not JSON"
}


$url = "https://example.com"
$headers = @{
    "HeaderName1" = "HeaderValue1"
    "HeaderName2" = "HeaderValue2"
}
$certPath = "C:\path\to\client.crt"
$keyPath = "C:\path\to\client.key"
$caPath = "C:\path\to\ca.crt"
$pfxPath = "C:\path\to\client.pfx"
$certPassword = "your_certificate_password" # This can be empty if no password is needed

# Combine the certificate and key into a PFX file
$cert = [System.Security.Cryptography.X509Certificates.X509Certificate2]::CreateFromCertFile($certPath)
$key = Get-Content -Path $keyPath -Raw
$ca = [System.Security.Cryptography.X509Certificates.X509Certificate2]::CreateFromCertFile($caPath)

$pfx = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2
$pfx.Import($cert.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert), $key, [System.Security.Cryptography.X509Certificates.X509KeyStorageFlags]::DefaultKeySet)
$pfx.Import($ca.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Cert))

$pfxBytes = $pfx.Export([System.Security.Cryptography.X509Certificates.X509ContentType]::Pfx, $certPassword)
[System.IO.File]::WriteAllBytes($pfxPath, $pfxBytes)

# Load the client certificate from the PFX file
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2
$cert.Import($pfxPath, $certPassword, [System.Security.Cryptography.X509Certificates.X509KeyStorageFlags]::DefaultKeySet)

# Make the HTTP request with mTLS
$response = Invoke-WebRequest -Uri $url -UseDefaultCredentials -Headers $headers -Certificate $cert

# Get the HTTP status code
$statusCode = $response.StatusCode
Write-Output "HTTP Status Code: $statusCode"

# Function to get a nested property from a JSON object
function Get-NestedProperty {
    param (
        [Parameter(Mandatory=$true)]
        [psobject]$JsonObject,
        [Parameter(Mandatory=$true)]
        [string]$Path
    )
    $properties = $Path.Trim(':').Split('.')
    $current = $JsonObject
    foreach ($property in $properties) {
        if ($null -ne $current) {
            $current = $current.$property
        } else {
            return $null
        }
    }
    return $current
}

# Check if the content is JSON and parse it
if ($response.ContentType -eq "application/json") {
    $jsonContent = $response.Content | ConvertFrom-Json
    # Access a specific field from the JSON using a path similar to jq
    $path = ":a.b"  # Example path
    $specificField = Get-NestedProperty -JsonObject $jsonContent -Path $path
    Write-Output "Specific Field: $specificField"
} else {
    Write-Output "Content is not JSON"
}


# openssl pkcs12 -export -out client.pfx -inkey client.key -in client.crt -certfile ca.crt
# openssl pkcs12 -export -out client.pfx -inkey client.key -in client.crt -certfile ca.crt -passout pass:your_password