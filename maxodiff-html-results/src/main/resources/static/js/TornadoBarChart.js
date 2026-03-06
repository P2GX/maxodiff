
var rankedOmimTerms = rankedOmimTerms
var nDiseases = nDiseases
var idx = chartIdx;

var tornadoBarChart = document.getElementById('TornadoBarChartContainer_' + idx);
//console.log("TornadoBarChart = " + tornadoBarChart);

var minRankChange = -(nDiseases - 1)
var maxRankChange = nDiseases - 1

var diseaseLabels = getDiseaseLabels(rankedOmimTerms)
var rankChanges = getRankChanges(rankedOmimTerms)


function getDiseaseLabels(rankedOmimTerms) {
    var diseaseNames = [];
    for (let rankedOmimTerm of rankedOmimTerms) {
        var diseaseLabel = rankedOmimTerm.omimTerm.termLabel;
        diseaseNames.push(diseaseLabel);
    }

    return diseaseNames;
}


function getRankChanges(rankedOmimTerms) {
    var rankChanges = [];
    for (let rankedOmimTerm of rankedOmimTerms) {
        var initialRank = rankedOmimTerm.initialRank;
        var averageRank = rankedOmimTerm.averageRank;
        var rankChange = initialRank - averageRank;
        rankChanges.push(rankChange);
    }

    return rankChanges;
}

function getBarColors(rankChanges) {
    var colors = [];
    for (let rankChange of rankChanges) {
        if (rankChange >= 0) {
            colors.push('#FF4560') //red for rank decline
        } else {
            colors.push('#008FFB') // blue for rank improvement
        }
    }

    return colors;
}



var options = {
      series: [{
          name: 'Average Rank Change',
          data: rankChanges
      }],
      chart: {
          type: 'bar',
          height: 500
      },
      colors: getBarColors(rankChanges),
      plotOptions: {
        bar: {
          borderRadius: 4,
          borderRadiusApplication: 'end',
          horizontal: true,
          distributed: true
        }
      },
      dataLabels: {
        enabled: false
      },
      legend: {
        show: false
      },
      xaxis: {
        categories: diseaseLabels,
        min: minRankChange,
        max: maxRankChange,
        title: {
          text: 'Average Rank Change'
        },
      },
      tooltip: {
        y: {
            formatter: function(value, { series, seriesIndex, dataPointIndex, w }) {
              var seriesName = 'Decline';
              var value = series[seriesIndex][dataPointIndex];
              var rankChange = value;
              if (rankChange < 0) {
                  seriesName = 'Improve'
                  value = -rankChange;
              }

              return seriesName + " by " + value;
            }
        }
      }
};

var chart = new ApexCharts(tornadoBarChart, options);
chart.render();
