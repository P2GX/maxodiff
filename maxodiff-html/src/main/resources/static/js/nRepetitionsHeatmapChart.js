
var hpoTermIdRepCtsMap = hpoTermIdRepCtsMap
var maxoDiseaseAvgRankChangeMap = maxoDiseaseAvgRankChangeMap
var allHpoTermsMap = allHpoTermsMap;
var omimTerms = omimTerms;
var idx = chartIdx;

var heatmapChart = document.getElementById('nRepHeatmapChartContainer_' + idx);
console.log("nRepHeatmapChart = " + heatmapChart);

var hpoIdToLabelMap = htmlObjectToMap(allHpoTermsMap)
var repMap = htmlObjectToRepMap(hpoTermIdRepCtsMap);
var diseaseIds = getDiseaseIds(repMap);

var rankChangeMap = maxoDiseaseAvgRankChangeMap;
var rankChanges = getRankChanges(repMap, rankChangeMap)
var minRankChange = Math.min(...rankChanges)
var maxRankChange = Math.max(...rankChanges)

var data = getDatasetValues(repMap, rankChangeMap)


function htmlObjectToMap(htmlObject) {
  const map = new Map();
  for (const key in htmlObject) {
    if (htmlObject.hasOwnProperty(key)) {
      const value = htmlObject[key]
      map.set(key, value);
    }
  }
  return map;
}

function htmlObjectToRepMap(htmlObject) {
  const map = new Map();
  for (const key in htmlObject) {
    const ctMap = new Map();
    if (htmlObject.hasOwnProperty(key)) {
      const value = htmlObject[key]
      for (const key1 in value) {
        const value1 = value[key1]
        ctMap.set(key1, value1)
      }
      map.set(key, ctMap);
    }
  }
  return map;
}


function getDatasetValues(repMap, rankChangeMap) {
    var datasetValues = [];
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var diseaseLabel = omimTerms[diseaseIdKey]
        var ctMapDataset = ctMapValue
        var ctData = [{x: 'Average Rank Change', y: rankChangeMap[diseaseIdKey]}];
        for (let [hpoIdKey, repCtValue] of ctMapDataset) {
            var hpoLabel = allHpoTermsMap[hpoIdKey]
            var repCt = repCtValue
            var dataPt = {x: hpoLabel, y: repCt};
            ctData.push(dataPt);
        }
        datasetValues.push({name: diseaseLabel, data: ctData});
    }

    return datasetValues;
}

function getDiseaseIds(repMap) {
    var diseaseIds = [];
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var diseaseId = repMap[diseaseIdKey]
        diseaseIds.push(diseaseId);
    }

    return diseaseIds;
}


function getRankChanges(repMap, rankChangeMap) {
    var rankChanges = [];
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var rankChange = rankChangeMap[diseaseIdKey]
        rankChanges.push(rankChange);
    }

    return rankChanges;
}



var options = {
      series: data,
      chart: {
          height: 600,
          type: 'heatmap',
          events: {
            xAxisLabelClick: function(event, chartContext, opts) {
               console.log(opts)
               var labelIdx = opts.labelIndex;
               var label = opts.globals.labels[labelIdx]
               if (label != 'Average Rank Change') {
                  for (let [hpoIdKey, hpoLabelValue] of hpoIdToLabelMap) {
                      if (hpoLabelValue == label) {
                          var url = "https://hpo.jax.org/browse/term/" + hpoIdKey;
                          window.open(url);
                      }
                  }
               }
            }
          }
      },
      dataLabels: {
          enabled: false
      },
      colors: ["#ffffff"], //white default
      title: {
          text: 'HPO Term Repetition Counts'
      },
      plotOptions: {
          heatmap: {
              reverseNegativeShade: true,
              colorScale: {
                ranges: [{
                    from: minRankChange,
                    to: -1,
                    color: '#00A100', //green
                    //name: 'low',
                  },
                  {
                    from: 1,
                    to: maxRankChange,
                    color: '#FFB200', //gold
                    //name: 'high',
                  }
                ]
              }
          }
      }
};

var chart = new ApexCharts(heatmapChart, options);
chart.render();
