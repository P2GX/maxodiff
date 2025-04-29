
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

var repCtMultiplier = 100;

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
    var ctData = [{x: 'Average Rank Change', y: null}];
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var diseaseLabel = omimTerms[diseaseIdKey]
        var ctMapDataset = ctMapValue
        var rcData = [{x: 'Average Rank Change', y: rankChangeMap[diseaseIdKey]}];
        for (let [hpoIdKey, repCtValue] of ctMapDataset) {
            var hpoLabel = allHpoTermsMap[hpoIdKey]
            var repCt = repCtValue * repCtMultiplier
            if (repCt != null && repCt != 0) {
                var dataPt = {x: hpoLabel, y: repCt};
                ctData.push(dataPt);
            }
        }
        for (var i = 1; i < ctData.length; i++) {
            var ctDataPt = ctData[i];
            rcData.push({x: ctDataPt.x, y: null})
        }
        datasetValues.push({name: diseaseLabel, data: rcData});
    }
    datasetValues.push({name: 'N Repetitions', data: ctData});

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
      xaxis: {
        labels: {
          formatter: function(str) {
            const n = 25
            return str.length > n ? str.substr(0, n - 1) + '...' : str
          },
          style: {
            colors: xAxisLabelColors,
          },
        },
      },
      yaxis: {
        labels: {
          formatter: function(str) {
            const n = 30
            return str.length > n ? str.substr(0, n - 1) + '...' : str
          }
        }
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
                    from: repCtMultiplier,
                    to: nRepetitions * repCtMultiplier,
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
            var hpoLabel = data.x;
            var y = data.y;

            if (y >= minRankChange && y <= -1) {
                return '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap; background-color: lightgray; color: blue">' +
                       '<b>Disease Term</b>: ' + omimLabel + '</div>' +
                       '<div><p></p></div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap;">' +
                       '<b>Average Rank Improvement</b>: ' + -y + '</div>';
            } else if (y >= 1 && y <= maxRankChange) {
                return '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap; background-color: lightgray; color: blue">' +
                       '<b>Disease Term</b>: ' + omimLabel + '</div>' +
                       '<div><p></p></div>' +
                       '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap;">' +
                       '<b>Average Rank Improvement</b>: ' + y + '</div>';
            } else if (y >= repCtMultiplier) {
                frequencyMap = new Map()
                for (let [hpoIdKey, hpoLabelValue] of hpoIdToLabelMap) {
                    if (hpoLabelValue == hpoLabel) {
                        var hpoId = hpoIdKey
                        for (let [omimIdKey, omimLabelValue] of omimIdToLabelMap) {
                            var omimId = omimIdKey
                            var hpoRepCt = hpoTermIdRepCtsMap[omimId][hpoId]
                            if (hpoRepCt != null) {
                                for (i = 0; i < hpoFrequencies.length; i++) {
                                    var freqRecord = hpoFrequencies[i]
                                    var freqRecordOmimId = freqRecord.omimId
                                    var freqRecordHpoId = freqRecord.hpoId
                                    var frequency = freqRecord.frequency
                                    if (freqRecordOmimId == omimId && freqRecordHpoId == hpoId && frequency != null) {
                                        if (!frequencyMap.has(frequency)) {
                                            frequencyMapDiseaseList = [omimLabelValue]
                                            frequencyMap.set(frequency, frequencyMapDiseaseList)
                                        } else {
                                            frequencyMapDiseaseList = frequencyMap.get(frequency)
                                            frequencyMapDiseaseList.push(omimLabelValue)
                                            frequencyMap.set(frequency, frequencyMapDiseaseList)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                var freqHTML = ''
                for (let [frequency, omimLabels] of frequencyMap) {
                  omimLabelString = omimLabels.join("; ")
                  freqHTML += '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap; ">' +
                                 '<b>Frequency of ' + '<span style="color: red">' + hpoLabel + '</span>' +
                                 ' in ' + '<span style="color: blue">' + omimLabelString + '</span>' +
                                 '</b>: ' + frequency + '</div>' +
                              '<div><p></p></div>'
                }

//                return '<div style="background-color: lightgray; color: blue"><b>Disease Term</b>: ' + omimLabel + '</div>' +
                return '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap; background-color: lightgray; color: red">' +
                       '<b>HPO Term</b>: ' + hpoLabel + '</div>' +
                       '<div><p></p></div>' + freqHTML +
                       '<div style="font-family: Arial, Helvetica, sans-serif; white-space: pre-wrap; background-color: gold">' +
                       '<b>Repetition Count</b>: ' + (y/repCtMultiplier) + '</div>';
            }
          }
      }
};

var chart = new ApexCharts(heatmapChart, options);
chart.render();
