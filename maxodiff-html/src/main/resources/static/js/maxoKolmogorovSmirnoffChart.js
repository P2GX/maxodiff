//const posttestChart = document.getElementById(id);//'maxoKolmogorovSmirnoffChart'

var initialCDF = initialCDF;
var maxoCDF = maxoCDF;
var maxoId = maxoId;
var idx = chartIdx;

var posttestChart = document.getElementById('maxoKolmogorovSmirnoffChart_' + idx);
console.log("postestChart = " + posttestChart);

var cdfs = [initialCDF, maxoCDF]
var labels = ["Initial", "Final"]

var data = {
  datasets: getDatasetValues(cdfs)
};

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

function getDatasetValues(cdfs) {
    var datasetValues = [];
    for (i = 0; i < cdfs.length; i++) {
        var distData = [];
        // center x values at CDF value of 0.5
        var cdfHalfUBPt = cdfs[i].findIndex(n => n >= 0.5);
        for (j = 0; j < cdfs[i].length; j++) {
            var dataPt = {x: j, y: cdfs[i][j]};
            distData.push(dataPt);
        }
        datasetValues.push({label: labels[i], data: distData});
    }

    return datasetValues;
}

function customTooltip(tooltipItems, data, cdfs) {
    var dataset = data.datasets[tooltipItems.datasetIndex];
    var x = Math.round(dataset.data[tooltipItems.dataIndex].x * 1000) / 1000;
    var y = Math.round(dataset.data[tooltipItems.dataIndex].y * 1000) / 1000;
    var diseaseRank = tooltipItems.dataIndex + 1;
    var diseaseId = cdfs[tooltipItems.datasetIndex][tooltipItems.dataIndex].diseaseId;

    return dataset.label + ': ' + " (" + x + ", " + y + ")  ";// + diseaseRank + ". " + diseaseId;
}

function chartConfig(dataValue, cdfs, tooltipTitle) {
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
              text: 'Score Cumulative Distribution'
          },
          tooltip: {
              callbacks: {
                  title: () => { return tooltipTitle; },
                  label: (tooltipItems) => customTooltip(tooltipItems, dataValue, cdfs)
              }
          }
        },
        scales: {
          x: {
            type: 'linear',
            position: 'bottom',
            title: {
                  display: true,
                  text: 'x',
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


new Chart(posttestChart, chartConfig(data, cdfs, maxoId));
