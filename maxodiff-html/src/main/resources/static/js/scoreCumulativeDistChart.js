const posttestChart = document.getElementById('posttestCumulativeDistributionChart');
const weightChart = document.getElementById('weightCumulativeDistributionChart');

var dataLabels = distributionDatasetLabels;
var cumulativeDistributionRecords = cumulativeDistributionRecords;
var posttestKey = posttestFilterKey;
var weightKey = weightKey;

var posttestLegendTitle = dataLabels.weight[1]
var weightLegendTitle = dataLabels.posttestFilter[1]


const posttestFilterData = {
  datasets: getDatasetValues(posttestKey)
};

const weightData = {
  datasets: getDatasetValues(weightKey)
};

const posttestFilterCumulativeDistributionRecords = cumulativeDistributionRecords.posttestFilter
const weightCumulativeDistributionRecords = cumulativeDistributionRecords.weight

var legendSpaceValue = 10;

var legendBorder = {
    id: "legendBorder",
    beforeDatasetsDraw(chart) {
        const { ctx, legend } = chart;

        const widthCenter = legend.width/2;

        legend.lineWidths.forEach((itemWidth, index) => {
            ctx.save();
            ctx.beginPath();
            ctx.moveTo(widthCenter - (itemWidth/2), legend.top - 5);
            ctx.lineTo(widthCenter + (itemWidth/2), legend.top - 5);
            ctx.lineTo(widthCenter + (itemWidth/2), legend.bottom - 5 - legendSpaceValue);
            ctx.lineTo(widthCenter - (itemWidth/2), legend.bottom - 5 - legendSpaceValue);
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
            return this.height += legendSpaceValue;
        }
    }
}

function getDatasetValues(key) {
    if (key == posttestKey) {
        var cumulativeDistributionRecordsList = cumulativeDistributionRecords.posttestFilter;
        var labelSublist = dataLabels.posttestFilter;
    } else if (key == weightKey) {
        var cumulativeDistributionRecordsList = cumulativeDistributionRecords.weight;
        var labelSublist = dataLabels.weight;
    }

    var datasetValues = [];
    for (i = 0; i < cumulativeDistributionRecordsList.length; i++) {
        var distData = [];
        for (j = 0; j < cumulativeDistributionRecordsList[i].length; j++) {
            var dataPt = {x: cumulativeDistributionRecordsList[i][j].score, y: cumulativeDistributionRecordsList[i][j].probability};
            distData.push(dataPt);
        }
        datasetValues.push({label: labelSublist[i], data: distData});
    }

    return datasetValues;
}

function customTooltip(tooltipItems, data, cumulativeDistributionRecordsList) {
    var dataset = data.datasets[tooltipItems.datasetIndex];
    var x = Math.round(dataset.data[tooltipItems.dataIndex].x * 1000) / 1000;
    var y = Math.round(dataset.data[tooltipItems.dataIndex].y * 1000) / 1000;
    var maxoRank = tooltipItems.dataIndex + 1;
    var maxoId = cumulativeDistributionRecordsList[tooltipItems.datasetIndex][tooltipItems.dataIndex].maxoId;
    var maxoLabel = cumulativeDistributionRecordsList[tooltipItems.datasetIndex][tooltipItems.dataIndex].maxoLabel;

    return dataset.label + ': ' + " (" + x + ", " + y + ")  " + maxoRank + ". " + maxoId + ": " + maxoLabel;
}

function chartConfig(dataValue, cumulativeDistributionRecordsList, tooltipTitle) {
    config = {
      type: 'scatter',
      data: dataValue,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
              position: 'top',
              title: {
                display: true,
                text: tooltipTitle,
                font: {
                    size: 10,
                    weight: 'bold'
                }
              },
              labels: {
                usePointStyle: true,
                boxHeight: 6,
                font: {
                    size: 10
                }
              }
          },
          title: {
              display: true,
              text: 'MaXo Score Cumulative Distribution'
          },
          tooltip: {
              callbacks: {
                  title: () => { return tooltipTitle; },
                  label: (tooltipItems) => customTooltip(tooltipItems, dataValue, cumulativeDistributionRecordsList)
              }
          }
        },
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            title: {
                  display: true,
                  text: 'MaXo Term Score',
                  font: {
                      size: 12
                  }
              }
          },
          y: {
              title: {
                  display: true,
                  text: 'Cumulative Probability',
                  font: {
                      size: 12
                  }
              }
          }
        }
      },
      plugins: [legendBorder, legendMargin]
    };
    return config;
}


new Chart(posttestChart, chartConfig(posttestFilterData, posttestFilterCumulativeDistributionRecords, posttestLegendTitle));
new Chart(weightChart, chartConfig(weightData, weightCumulativeDistributionRecords, weightLegendTitle));
