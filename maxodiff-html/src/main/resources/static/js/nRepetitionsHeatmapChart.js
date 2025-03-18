
var hpoTermIdRepCtsMap = hpoTermIdRepCtsMap
var nDiseases = nDiseases
var nRepetitions = nRepetitions
var maxoDiseaseAvgRankChangeMap = maxoDiseaseAvgRankChangeMap
var allHpoTermsMap = allHpoTermsMap
var omimTerms = omimTerms
var hpoTermCounts = hpoTermCounts
var idx = chartIdx;

var heatmapChart = document.getElementById('nRepHeatmapChartContainer_' + idx);
console.log("nRepHeatmapChart = " + heatmapChart);

var omimIdToLabelMap = htmlObjectToMap(omimTerms)
var hpoIdToLabelMap = htmlObjectToMap(allHpoTermsMap)
var repMap = htmlObjectToRepMap(hpoTermIdRepCtsMap);
var diseaseIds = getDiseaseIds(repMap);

var rankChangeMap = maxoDiseaseAvgRankChangeMap;
var minRankChange = -(nDiseases - 1)
var maxRankChange = nDiseases - 1

var hpoTermCountsMap = htmlObjectToHpoTermCountMap(hpoTermCounts)
var hpoFrequencies = getHpoFrequencies(hpoTermCountsMap)

var data = getDatasetValues(repMap, rankChangeMap)
var xAxisLabelColors = getXAxisLabelColors(repMap)


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


function htmlObjectToHpoTermCountMap(htmlObject) {
  const map = new Map();
  for (const key in htmlObject) {
    if (htmlObject.hasOwnProperty(key)) {
      const freqRecordListValue = htmlObject[key]
      map.set(key, freqRecordListValue);
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
            var repCt = repCtValue * 100
            var dataPt = {x: hpoLabel, y: repCt};
            ctData.push(dataPt);
        }
        datasetValues.push({name: diseaseLabel, data: ctData});
    }

    return datasetValues;
}

function getXAxisLabelColors(repMap) {
    var colors = ['black']
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var ctMapDataset = ctMapValue
        for (let [hpoIdKey, repCtValue] of ctMapDataset) {
            colors.push('blue');
        }
        break;
    }

    return colors;
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

function getHpoFrequencies(hpoTermCountsMap) {
    var freqRecords = [];
    for (let [hpoIdKey, freqRecordListValue] of hpoTermCountsMap) {
        var hpoId = hpoIdKey
        var freqRecordList = freqRecordListValue
        for (i = 0; i < freqRecordList.length; i++) {
            freqRecords.push(freqRecordList[i])
        }
    }

    return freqRecords;
}



var options = {
      series: data,
      chart: {
          height: 600,
          type: 'heatmap',
          events: {
             xAxisLabelClick: function(event, chartContext, opts) {
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
      xaxis: {
        labels: {
          style: {
            colors: xAxisLabelColors,
          },
        },
      },
      plotOptions: {
          heatmap: {
              reverseNegativeShade: true,
              colorScale: {
                ranges: [{
                    from: minRankChange,
                    to: -1,
                    color: '#00A100', //green
                    name: 'Rank Improvement',
                  },
                  {
                    from: 1,
                    to: maxRankChange,
                    color: '#FF0000', //red
                    name: 'Rank Decline',
                  },
                  {
                    from: 100,
                    to: nRepetitions * 100,
                    color: '#FFB200', //gold
                    name: 'Repetition Counts',
                  }
                ]
              }
          }
      },
      tooltip: {
          custom: function({series, seriesIndex, dataPointIndex, w}) {
            var data = w.globals.initialSeries[seriesIndex].data[dataPointIndex];
            var omimLabel = w.globals.initialSeries[seriesIndex].name;
            var x = data.x;
            var y = data.y;

            if (y >= minRankChange && y <= -1) {
                return '<div style="font-family: Arial, Helvetica, sans-serif;"><b>OMIM Term</b>: ' + omimLabel + '</div>' +
                       '<div></div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif;"><b>Average Rank Improvement</b>: ' + -y + '</div>';
            } else if (y >= 1 && y <= maxRankChange) {
                return '<div style="font-family: Arial, Helvetica, sans-serif;"><b>OMIM Term</b>: ' + omimLabel + '</div>' +
                       '<div></div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif;"><b>Average Rank Decline</b>: ' + y + '</div>';
            } else if (y >= 100) {
                frequency = 0
                for (let [omimIdKey, omimLabelValue] of omimIdToLabelMap) {
                    if (omimLabelValue == omimLabel) {
                        var omimId = omimIdKey
                        for (let [hpoIdKey, hpoLabelValue] of hpoIdToLabelMap) {
                            if (hpoLabelValue == x) {
                                var hpoId = hpoIdKey
                                for (i = 0; i < hpoFrequencies.length; i++) {
                                    var freqRecord = hpoFrequencies[i]
                                    var freqRecordOmimId = freqRecord.omimId
                                    var freqRecordHpoId = freqRecord.hpoId
                                    if (freqRecordOmimId == omimId && freqRecordHpoId == hpoId) {
                                        frequency = freqRecord.frequency
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                return '<div style="font-family: Arial, Helvetica, sans-serif;"><b>OMIM Term</b>: ' + omimLabel + '</div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif;"><b>HPO Term</b>: ' + x + '</div>' +
                       '<div></div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif;"><b>Repetition Count</b>: ' + (y/100) + '</div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif;"><b>Frequency</b>: ' + frequency + '</div>';
            }
          }
      }
};

var chart = new ApexCharts(heatmapChart, options);
chart.render();
