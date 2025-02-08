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
