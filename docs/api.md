# JSON API

Other programs can use maxodiff over HTTP. The server accepts a
[phenopacket](background.md#ga4gh-phenopackets) as JSON and returns the results
as JSON. This is the interface that SAMS uses.

!!! info "Pilot phase"
The JSON format is in a pilot stage. We are actively seeking feedback to
refine the data structure.

Start the [server](server.md) first. The examples assume that it runs on
`localhost:8080`.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/analyze` | Rank diagnostic procedures for a patient |
| `POST` | `/api/modality?targetDiseaseId=<id>` | Rank procedures with respect to one target disease |

Both endpoints expect the phenopacket as the body of the request, with the
header `Content-Type: application/json`. The phenopacket must have an `id`.

## Analyze a patient

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d @docs/examples/example-phenopacket.json \
  -o response.json
```

The example uses [`example-phenopacket.json`](examples/example-phenopacket.json).
The analysis takes about 30 seconds. 

The analysis uses the top **20** diseases and **80** repetitions. These values
cannot be changed through the API.

### Structure of the response

The response is one JSON object:

| Field | Meaning |
|---|---|
| `ppktId` | The `id` of the phenopacket |
| `nDiseases` | Number of diseases that were analyzed (20) |
| `nRepetitions` | Number of simulations per procedure (80) |
| `observedHpoTerms` | HPO terms that are observed in the patient |
| `excludedHpoTerms` | HPO terms that are excluded in the patient |
| `resultList` | One entry per MAxO procedure, **sorted by `maxoScore`, highest first** |

Every entry of `resultList` describes one procedure:

| Field | Meaning |
|---|---|
| `maxoTerm` | The MAxO procedure |
| `maxoScore` | The score (ΔScore): how much the procedure is expected to change the ranking. See [Reading the results](understanding.md#the-score-score). |
| `rankedOmimTermList` | The analyzed diseases. `averageRank` is the average rank of the disease over all simulations (rank 1 is the best). |
| `hpoTermIds` | HPO terms that the procedure could reveal. `count` is the number of simulations in which the term was drawn. |
| `frequencies` | For each pair of disease and revealable HPO term: `frequency`, the frequency of the term in patients with the disease, and `mica`, the information content of the term for that disease |

Terms are given by their identifier only, for example `{"tid": "MAXO:0000703"}`.
Labels are not part of the response.

Abridged example (shortened with `...`, so it is not valid JSON):

```json
{
  "ppktId": "PMID_36322151_KDM2B_23",
  "nDiseases": 20,
  "nRepetitions": 80,
  "observedHpoTerms": [ { "tid": "HP:0000122" }, { "tid": "HP:0001631" }, ... ],
  "excludedHpoTerms": [ { "tid": "HP:0011255" }, ... ],
  "resultList": [
    {
      "maxoTerm": { "tid": "MAXO:0000703" },
      "maxoScore": 12.343383583865394,
      "rankedOmimTermList": [
        { "tid": "OMIM:621220", "averageRank": 1.9 },
        { "tid": "OMIM:615829", "averageRank": 4.0125 },
        ...
      ],
      "hpoTermIds": [
        { "tid": "HP:0000540", "count": 20 },
        { "tid": "HP:0000565", "count": 17 },
        ...
      ],
      "frequencies": [
        { "diseaseId": "OMIM:621570", "hpoId": "HP:0000540", "frequency": 0.25, "mica": 3.6826675 },
        ...
      ]
    },
    ...
  ]
}
```

### Things to know

- **The initial rank is not in the response.** `rankedOmimTermList` lists the
  diseases in the order of the initial differential diagnosis. The position in
  the list (starting at 1) is the initial rank. The change of a disease is
  `averageRank` minus its position. The same order is used in every entry.
- **All procedures are returned, also those with score 0.** In the example
  response, 186 of 312 procedures have a score of 0. They cannot change the
  ranking. Skip them in your program.
- **The response can be large** (about 0.5 MB in the example).

## Best procedures for one disease

If you suspect one disease, ask for the procedures that help most to confirm or
exclude it:

```bash
curl -X POST "http://localhost:8080/api/modality?targetDiseaseId=OMIM:157700" \
  -H "Content-Type: application/json" \
  -d @docs/examples/example-phenopacket.json
```

The target disease id is required and can be an OMIM id (substitute any OMIM
id). The response is a list, sorted by `maxoScore`, highest first:

```json
[
  {
    "targetDisease": { "tid": "OMIM:157700" },
    "maxoTerm": { "tid": "MAXO:0035050" },
    "nMaxoPhenotypes": 1,
    "totalIC": 9.061724347647399,
    "maxoScore": 8.499235179380303
  },
  {
    "targetDisease": { "tid": "OMIM:157700" },
    "maxoTerm": { "tid": "MAXO:0000706" },
    "nMaxoPhenotypes": 2,
    "totalIC": 7.263296516746243,
    "maxoScore": 16.172687354838573
  }
]
```

| Field | Meaning |
|---|---|
| `targetDisease` | The disease that you asked for |
| `maxoTerm` | The MAxO procedure |
| `nMaxoPhenotypes` | Number of phenotypes of the target disease that the procedure can reveal |
| `totalIC` | Sum of the information content of those phenotypes |
| `maxoScore` | The score of the procedure for the target disease |

The values in the example are illustrative.

## Errors

| Status | Meaning |
|---|---|
| `200` | The analysis was done. |
| `400` | The request has no phenopacket or the phenopacket has no `id`. |

You can also get the JSON through the [web page](server.md#use-the-web-page):
choose **JSON** as the output.