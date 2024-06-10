
var idx = maxoIdx;
var maxoResults = maxoRecs;
var initialScores = initScores;
var allMaxoTermsMap = allMaxoTermMap;

var chart = document.getElementById('chart' + idx);

var chartData = {
  labels: Object.keys(initialScores),
  datasets: getDatasets()
};

var extendLine = {
    id: "extendLine",

    afterDatasetsDraw(chart) {
        const { ctx } = chart;
        chart.data.datasets.forEach((dataset, index) => {
             if (dataset.type === 'line') {
                  var xaxis = chart.scales.x;
                  var yaxis = chart.scales.y;

                  firstPt = chart.getDatasetMeta(index).dataset._points[0];
                  lastPt = chart.getDatasetMeta(index).dataset._points[dataset.data.length - 1];

                  //first data pt position in pixel coords
                  var x1 = xaxis.left;
                  var y1 = firstPt.y;
                  var x2 = firstPt.x;
                  var y2 = firstPt.y

                  //last data pt position in pixel coords
                  var x3 = lastPt.x;
                  var y3 = lastPt.y;
                  var x4 = xaxis.right;
                  var y4 = lastPt.y;

                  ctx.save();
                  ctx.strokeStyle = dataset.borderColor;
                  ctx.lineWidth = dataset.borderWidth;
                  ctx.beginPath();
                  ctx.moveTo(x1, y1);
                  ctx.lineTo(x2, y2);
                  ctx.stroke();
                  ctx.beginPath();
                  ctx.moveTo(x3, y3);
                  ctx.lineTo(x4, y4);
                  ctx.stroke();
             }
        });
    }
}

var legendOffset = 10;

var legendBorder = {
    id: "legendBorder",
    beforeDatasetsDraw(chart) {
        const { ctx, legend } = chart;

        const widthCenter = legend.width/2;

        legend.lineWidths.forEach((itemWidth, index) => {
            ctx.save();
            ctx.strokeStyle = 'black';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(widthCenter - (itemWidth/2), legend.top - 5);
            ctx.lineTo(widthCenter + (itemWidth/2), legend.top - 5);
            ctx.lineTo(widthCenter + (itemWidth/2), legend.bottom - 5 - legendOffset);
            ctx.lineTo(widthCenter - (itemWidth/2), legend.bottom - 5 - legendOffset);
            ctx.closePath();
            ctx.stroke();
        })
    }
}

var legendMargin = {
    id: "legendMargin",
    beforeInit(chart) {
        const fitValue = chart.legend.fit;

        chart.legend.fit = function fit() {
            fitValue.bind(chart.legend)();
            return this.height += legendOffset;
        }
    }
}

function getDatasets() {

    var datasets = [];
    for (i = 0; i < 2; i++) {
        var distData = [];
        var dataLabel = "";
        probabilityMap = initialScores;
        diseaseIds = Object.keys(probabilityMap);
        if (i == 0) {
            dataLabel = "Initial Posttest Probability (placeholder)";
            for (j = 0; j < diseaseIds.length; j++) {
                var initialScore = 0.5; //maxoRecords[j].initialScore;
                var dataPt = initialScore; //{x: j, y: initialScore};
                distData.push(dataPt);
            }
            datasets.push({type: 'line',
                           label: dataLabel,
                           data: distData,
                           borderColor: 'black',
                           pointBackgroundColor: 'darkgray',
                           pointRadius: 0,
                           showLine: true,
                           borderWidth: 2});
        } else if (i == 1) {
            var borderColors = [];
            var borderSkips = [];
            var bkgColors = [];
            var ptStyles = [];
            dataLabel = "Final Posttest Probability";
            for (j = 0; j < diseaseIds.length; j++) {
                var initialScore = 0.5; //maxoRecords[j].score1;
                var finalScore = probabilityMap[diseaseIds[j]];
                var dataPt = finalScore; //{x: j, y: finalScore};
                distData.push(dataPt);
                if (finalScore >= initialScore) {
                    bkgColors.push('lightgreen')
                    borderColors.push('green')
                    borderSkips.push('bottom')
//                    ptStyles.push('circle')
                } else if (finalScore < initialScore) {
                    bkgColors.push('pink')
                    borderColors.push('red')
                    borderSkips.push('top')
//                    ptStyles.push('triangle')
                }
            }
            datasets.push({type: 'bar',
                          label: dataLabel,
                          data: distData.map(v => [initialScore, v]), //v < initialScore ? [v, initialScore] : [initialScore, v]
                          backgroundColor: bkgColors,
                          borderColor: borderColors,
                          borderWidth: 2,
                          borderSkipped: borderSkips}); //, pointStyle: ptStyles});
        }
    }

    return datasets;
}

function customLegend(chart) {
     let pointStyle = [];
     let bkgColors = [];
     let borderColors = [];
     chart.data.datasets.forEach(dataset => {
         if (dataset.type === 'line') {
             bkgColors.push(dataset.backgroundColor);
             borderColors.push(dataset.borderColor);
             pointStyle.push('line');
         } else {
             bkgColors.push('lightgreen');
             borderColors.push('green');
             pointStyle.push('rect');
         }
     });

     return chart.data.datasets.map(
         (dataset, index) => ({text: dataset.label,
                               fillStyle: bkgColors[index],
                               strokeStyle: borderColors[index],
                               lineWidth: dataset.borderWidth,
                               pointStyle: pointStyle[index]}))
}

function customTooltip(tooltipItems, data, initialScores) {
    var dataset = data.datasets[tooltipItems.datasetIndex];
    var initialProbability = data.datasets[0].data[tooltipItems.datasetIndex];
    var probabilityMap = initialScores;
    var probabilities = Object.values(probabilityMap);
    var probability = tooltipItems.datasetIndex > 0 ? probabilities[tooltipItems.dataIndex] : data.datasets[0].data[tooltipItems.datasetIndex];
    probability = Math.round(probability * 1000) / 1000;
    var probabilityDiff = Math.round((probability - initialProbability) * 1000) / 1000;
    var diseaseIds = Object.keys(probabilityMap);
    var diseaseRank = tooltipItems.dataIndex + 1;
    var diseaseId = diseaseIds[tooltipItems.dataIndex];

    return dataset.label + ': ' + probability + " (\u0394 = " + probabilityDiff + ") " + diseaseRank + ". " + diseaseId;
}


function chartConfig(dataValue, initialScores) {
    config = {
//      type: 'scatter',
      data: dataValue,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
              position: 'top',
              title: {
                display: false,
//                text: tooltipTitle,
                font: {
                    size: 10,
                    weight: 'bold'
                }
              },
              labels: {
                usePointStyle: true,
//                boxHeight: 6,
                font: {
                    size: 10
                },
                generateLabels: (chart) => customLegend(chart)
              }
          },
          title: {
              display: true,
              text: idx + ') ' + maxoResults[idx-1].maxoTermScore.maxoId + ": " + allMaxoTermsMap[maxoResults[idx-1].maxoTermScore.maxoId]
          },
          tooltip: {
              callbacks: {
//                  title: () => { return tooltipTitle; },
                  label: (tooltipItems) => customTooltip(tooltipItems, dataValue, initialScores)
              }
          }
        },
        scales: {
          x: {
//            type: 'linear',
            position: 'bottom',
            title: {
                  display: true,
                  text: 'Disease ID',
                  font: {
                      size: 12
                  }
              },
            ticks: {
                  font: {
                    size: 10
                }
            }
          },
          y: {
              title: {
                  display: true,
                  text: 'Posttest Probability',
                  font: {
                      size: 12
                  }
              }
          }
        }
      },
      plugins: [extendLine, legendBorder, legendMargin]
    };
    return config;
}


new Chart(chart, chartConfig(chartData, initialScores));

