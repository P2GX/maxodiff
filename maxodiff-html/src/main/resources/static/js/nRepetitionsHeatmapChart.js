
var hpoTermIdRepCtsMap = hpoTermIdRepCtsMap
var hpoTermIdExcludedRepCtsMap = hpoTermIdExcludedRepCtsMap
var nDiseases = nDiseases
var nRepetitions = nRepetitions
var maxoDiseaseAvgRankChangeMap = maxoDiseaseAvgRankChangeMap
var allHpoTermsMap = allHpoTermsMap
var omimTerms = omimTerms
var hpoTermCounts = hpoTermCounts
var frequencyMap = frequencyDiseaseMap
var idx = chartIdx;

var heatmapChart = document.getElementById('nRepHeatmapChartContainer_' + idx);
console.log("nRepHeatmapChart = " + heatmapChart);

var repCtMultiplier = 100;
var annotMultiplier = 100 * repCtMultiplier;

var hpoIdToLabelMap = htmlObjectToMap(allHpoTermsMap)
var repMap = htmlObjectToRepMap(hpoTermIdRepCtsMap);
var excludedRepMap = htmlObjectToRepMap(hpoTermIdExcludedRepCtsMap);
var diseaseIds = getDiseaseIds(repMap);

var rankChangeMap = maxoDiseaseAvgRankChangeMap;
var minRankChange = -(nDiseases - 1)
var maxRankChange = nDiseases - 1

var hpoTermCountsMap = htmlObjectToHpoTermCountMap(hpoTermCounts)
var hpoFrequencies = getHpoFrequencies(hpoTermCountsMap)
var frequencyDiseaseMap = htmlObjectToRepMap(frequencyMap)

var data = getDatasetValues(repMap, excludedRepMap, rankChangeMap)
var xAxisLabelColors = getXAxisLabelColors(repMap, excludedRepMap)


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

function reorderArray(arrayToReorder, orderedArray) {
  const order = orderedArray.map(point => point.x);
  const reorderedPts = [];
  const ptsMap = new Map(arrayToReorder.map(pt => [pt.x, pt]));

  for (const x of order) {
    if (ptsMap.has(x)) {
      reorderedPts.push(ptsMap.get(x));
    }
  }

  return reorderedPts;
}


function getDatasetValues(repMap, excludedRepMap, rankChangeMap) {
    var datasetValues = [];
    var ctData = [{x: 'Average Rank Change', y: null}];
    datasetValues.push({name: 'N Repetitions', data: ctData});
    for (let [diseaseIdKey, ctMapValue] of repMap) {
        var diseaseLabel = omimTerms[diseaseIdKey]
        var ctMapDataset = ctMapValue
        var rcData = [{x: 'Average Rank Change', y: rankChangeMap[diseaseIdKey]}];
        for (let [hpoIdKey, repCtValue] of ctMapDataset) {
            var hpoLabel = allHpoTermsMap[hpoIdKey]
            var repCt = repCtValue * repCtMultiplier
            rcData.push({x: hpoLabel, y: null})
            if (repCt != null && repCt != 0) {
                var dataPt = {x: hpoLabel, y: repCt};
                const exists = ctData.find(p => p.x === dataPt.x && p.y === dataPt.y) !== undefined;
                if (!exists) {
                    ctData.push(dataPt);
                }
                for (i = 0; i < hpoFrequencies.length; i++) {
                    var freqRecord = hpoFrequencies[i]
                    var freqRecordOmimId = freqRecord.omimId
                    var freqRecordHpoId = freqRecord.hpoId
                    if (freqRecordOmimId == diseaseIdKey && freqRecordHpoId == hpoIdKey) {
                        var newHpoAnnotPt = {x: hpoLabel, y: annotMultiplier}
                        var idx = rcData.indexOf({x: hpoLabel, y: null});
                        rcData.splice(idx, 1, newHpoAnnotPt);
                        break;
                    }
                }
            }
        }
        rcData = reorderArray(rcData, ctData)
        datasetValues.push({name: diseaseLabel, data: rcData});
    }
    for (let [diseaseIdKey, ctMapValue] of excludedRepMap) {
        var diseaseLabel = omimTerms[diseaseIdKey]
        var ctMapDataset = ctMapValue
        for (let [hpoIdKey, repCtValue] of ctMapDataset) {
            var hpoLabel = allHpoTermsMap[hpoIdKey]
            var repCt = repCtValue * repCtMultiplier
            rcData.push({x: hpoLabel, y: null})
            if (repCt != null && repCt != 0) {
                var dataPt = {x: hpoLabel, y: repCt};
                const exists = ctData.find(p => p.x === dataPt.x && p.y === dataPt.y) !== undefined;
                if (!exists) {
                    ctData.push(dataPt);
                }
                for (i = 0; i < hpoFrequencies.length; i++) {
                    var freqRecord = hpoFrequencies[i]
                    var freqRecordOmimId = freqRecord.omimId
                    var freqRecordHpoId = freqRecord.hpoId
                    if (freqRecordOmimId == diseaseIdKey && freqRecordHpoId == hpoIdKey) {
                        var newHpoAnnotPt = {x: hpoLabel, y: annotMultiplier}
                        var idx = rcData.indexOf({x: hpoLabel, y: null});
                        rcData.splice(idx, 1, newHpoAnnotPt);
                        break;
                    }
                }
            }
        }
        rcData = reorderArray(rcData, ctData)
    }
    console.log(datasetValues)

    return datasetValues;
}

function getXAxisLabelColors(repMap, excludedRepMap) {
    var colors = ['black']
    for (let [diseaseIdKey, ctMapValue] of excludedRepMap) {
            var ctMapDataset = ctMapValue
            for (let [hpoIdKey, repCtValue] of ctMapDataset) {
                colors.push('blue');
            }
            break;
        }
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


function getRankChanges(rankChangeMap) {
    var rankChanges = [];
    for (let [diseaseIdKey, rankChangeValue] of rankChangeMap) {
        rankChanges.push(rankChangeValue);
    }

    return rankChanges;
}

function getHpoFrequencies(hpoTermCountsMap) {
    var freqRecords = [];
    for (let [hpoIdKey, freqRecordListValue] of hpoTermCountsMap) {
        var hpoId = hpoIdKey
        var freqRecordList = freqRecordListValue
        for (i = 0; i < freqRecordList.length; i++) {
            var freqRecord = freqRecordList[i]
            if (freqRecord.frequency != null) {
                freqRecords.push(freqRecord)
            }
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
      grid: {
          yaxis: {
            lines: {
              show: false
            }
          }
      },
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
                    from: -nRepetitions * repCtMultiplier,
                    to: -repCtMultiplier,
                    color: '#87CEFA', //light sky blue
                    name: 'Repetition Counts (Excluded)',
                  },
                  {
                    from: repCtMultiplier,
                    to: nRepetitions * repCtMultiplier,
                    color: '#FFB200', //gold
                    name: 'Repetition Counts (Observed)',
                  },
                  {
                    from: annotMultiplier,
                    to: annotMultiplier,
                    color: '#800080', //purple
                    name: 'Disease Annotation',
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
                return '<div style="background-color: lightgray; color: blue"><b>Disease Term</b>: ' + omimLabel + '</div>' +
                       '<div><p></p></div>' +
                       '<div><b>Average Rank Improvement</b>: ' + -y + '</div>';
            } else if (y >= 1 && y <= maxRankChange) {
                return '<div style="background-color: lightgray; color: blue"><b>Disease Term</b>: ' + omimLabel + '</div>' +
                       '<div><p></p></div>' +
                       '<div><b>Average Rank Decline</b>: ' + y + '</div>';
            } else if ((y >= repCtMultiplier && y < annotMultiplier) | y <= -repCtMultiplier) {
                var freqHTML = ''
                for (let [hpoLabelKey, frequencyMapValue] of frequencyDiseaseMap) {
                    if (hpoLabelKey == hpoLabel) {
                        for (let [frequency, omimLabels] of frequencyMapValue) {
                          omimLabelString = omimLabels.join("; ")
                          freqHTML += '<div><b>Frequency of ' + '<span style="color: red">' + hpoLabel + '</span>' +
                                         ' in ' + '<span style="color: blue">' + omimLabelString + '</span>' +
                                         '</b>: ' + frequency + '</div>' +
                                      '<div><p></p></div>'
                        }
                    }
                }

                if (y >= repCtMultiplier) {
                    return '<div style="background-color: lightgray; color: red"><b>HPO Term</b>: ' + hpoLabel + '</div>' +
                           '<div><p></p></div>' + freqHTML +
                           '<div style="background-color: gold"><b>Repetition Count</b>: ' + (y/repCtMultiplier) + ' of ' + nRepetitions + '</div>';
                } else if (y <= -repCtMultiplier) {
                    return '<div style="background-color: lightgray; color: red"><b>HPO Term</b>: ' + hpoLabel + '</div>' +
                           '<div><p></p></div>' + freqHTML +
                           '<div style="background-color: lightskyblue"><b>Repetition Count</b>: ' + (-y/repCtMultiplier) + ' of ' + nRepetitions + '</div>';
                }

            }
          }
      }
};

var chart = new ApexCharts(heatmapChart, options);
chart.render();
