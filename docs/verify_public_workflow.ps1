$ErrorActionPreference = 'Stop'

$base = 'http://124.223.111.253/api'

function Call-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri    = "$base$Path"
    }

    if ($null -ne $Body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }

    Invoke-RestMethod @params
}

$contractsBefore = Call-Api -Method 'GET' -Path '/contracts?page=0&size=20'
$tasksBefore = Call-Api -Method 'GET' -Path '/tasks'

$createdContract = Call-Api -Method 'POST' -Path '/contracts' -Body @{
    name         = 'public-api-check-contract'
    contractType = 'service-contract'
    partyA       = 'party-a-public-check'
    partyB       = 'party-b-public-check'
    amount       = 12345.67
    content      = 'public contract content for mysql persistence verification'
    source       = 'PUBLIC_VERIFY'
    status       = 'DRAFT'
}

$createdContractId = $createdContract.data.id
$fetchedContract = Call-Api -Method 'GET' -Path "/contracts/$createdContractId"

$consultation = Call-Api -Method 'POST' -Path '/legal/consultation' -Body @{
    question = 'What should an employer keep as evidence when ending a probation contract?'
    facts    = @(
        'location shanghai',
        'probation period two months',
        'need a short compliance checklist'
    )
}

$draft = Call-Api -Method 'POST' -Path '/legal/contract-draft' -Body @{
    contractName = 'public-api-generated-contract'
    contractType = 'technical-service-contract'
    partyA       = 'public-check-party-a'
    partyB       = 'public-check-party-b'
    amount       = 88888
    duration     = '12 months'
    requirements = 'include payment confidentiality breach liability and dispute resolution'
}

$review = Call-Api -Method 'POST' -Path '/legal/contract-review' -Body @{
    contractTitle   = 'public-api-review-contract'
    contractContent = 'Party A hires Party B for technical service. The contract misses confidentiality and dispute resolution clauses and does not clearly define acceptance standards.'
}

$tasksAfter = Call-Api -Method 'GET' -Path '/tasks'
$contractsAfter = Call-Api -Method 'GET' -Path '/contracts?page=0&size=20'

$beforeTaskNos = @($tasksBefore.data | ForEach-Object { $_.taskNo })
$newTasks = @($tasksAfter.data | Where-Object { $_.taskNo -notin $beforeTaskNos })

$report = [ordered]@{
    contractsBefore = $contractsBefore.data.totalElements
    contractsAfter  = $contractsAfter.data.totalElements
    createdContract = [ordered]@{
        id                  = $createdContract.data.id
        contractNo          = $createdContract.data.contractNo
        fetchedContentMatch = ($fetchedContract.data.content -eq 'public contract content for mysql persistence verification')
        fetchedSource       = $fetchedContract.data.source
        fetchedStatus       = $fetchedContract.data.status
    }
    consultation = [ordered]@{
        category            = $consultation.data.category
        legalBasisCount     = @($consultation.data.legalBasis).Count
        recommendationCount = @($consultation.data.recommendations).Count
    }
    contractDraft = [ordered]@{
        title         = $draft.data.title
        contentLength = $draft.data.generatedContent.Length
    }
    contractReview = [ordered]@{
        summaryLength      = $review.data.summary.Length
        riskCount          = @($review.data.risks).Count
        missingClauseCount = @($review.data.missingClauses).Count
    }
    tasksBefore = @($tasksBefore.data).Count
    tasksAfter  = @($tasksAfter.data).Count
    newTasks    = @($newTasks | Select-Object taskNo, title, type, relatedId, status)
}

$report | ConvertTo-Json -Depth 8
