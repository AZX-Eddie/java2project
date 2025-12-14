// 全局变量存储图表实例
let trendsChart = null;
let cooccurrenceChart = null;
let pitfallPieChart = null;
let pitfallBarChart = null;
let exceptionChart = null;
let solvabilityPieChart = null;
let factorsChart = null;
let comparisonChart = null;
let timingChart = null;

// 颜色配置
const COLORS = [
    '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
    '#FF9F40', '#FF6384', '#C9CBCF', '#7BC225', '#E83E8C',
    '#20C997', '#6610F2', '#FD7E14', '#17A2B8', '#28A745'
];

// ==================== 数据统计 ====================
function loadStats() {
    fetch('/admin/stats')
        .then(response => response.json())
        .then(result => {
            if (result.data) {
                const statsText = result.data;
                const matches = statsText.match(/(\d+)/g);
                if (matches && matches.length >= 3) {
                    document.getElementById('questionCount').textContent = matches[0];
                    document.getElementById('answerCount').textContent = matches[1];
                    document.getElementById('commentCount').textContent = matches[2];
                }
            }
        })
        .catch(error => {
            console.error('加载统计数据失败:', error);
        });
}

// ==================== 问题1: 主题趋势 ====================
function loadTopics() {
    fetch('/api/topics')
        .then(response => response.json())
        .then(topics => {
            const select = document.getElementById('trendTopic');
            topics.forEach(topic => {
                const option = document.createElement('option');
                option.value = topic;
                option.textContent = topic;
                select.appendChild(option);
            });
        })
        .catch(error => console.error('加载主题列表失败:', error));
}

function loadTrends() {
    const years = document.getElementById('trendYears').value;
    fetch(`/api/trends?years=${years}`)
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderTrendsChart(result.data);
                renderTrendsTable(result.data);
            }
        })
        .catch(error => console.error('加载趋势数据失败:', error));
}

function loadSingleTrend() {
    const topic = document.getElementById('trendTopic').value;
    const years = document.getElementById('trendYears').value;

    if (topic === 'all') {
        loadTrends();
        return;
    }

    fetch(`/api/trends/${topic}?years=${years}`)
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderTrendsChart([result.data]);
                renderTrendsTable([result.data]);
            }
        })
        .catch(error => console.error('加载单主题趋势失败:', error));
}

// 🔥 修改后：使用相对热度替代问题数量
function renderTrendsChart(data) {
    const ctx = document.getElementById('trendsChart').getContext('2d');

    if (trendsChart) {
        trendsChart.destroy();
    }

    // 收集所有月份
    const allMonths = new Set();
    data.forEach(topic => {
        if (topic.monthlyHeatIndex) {
            Object.keys(topic.monthlyHeatIndex).forEach(m => allMonths.add(m));
        }
    });
    const labels = Array.from(allMonths).sort();

    // 构建数据集（使用热度指数）
    const datasets = data.slice(0, 8).map((topic, index) => ({
        label: topic.topic,
        data: labels.map(month => {
            return topic.monthlyHeatIndex ? (topic.monthlyHeatIndex[month] || 0) : 0;
        }),
        borderColor: COLORS[index % COLORS.length],
        backgroundColor: COLORS[index % COLORS.length] + '20',
        tension: 0.4,
        fill: true,
        borderWidth: 2,
        pointRadius: 3,
        pointHoverRadius: 5
    }));

    trendsChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        padding: 15,
                        font: { size: 12 }
                    }
                },
                title: {
                    display: true,
                    text: 'Java主题月度相对热度趋势（综合指标 0-100）',
                    font: { size: 16, weight: 'bold' },
                    padding: 20
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleFont: { size: 14 },
                    bodyFont: { size: 13 },
                    callbacks: {
                        title: function(tooltipItems) {
                            return '月份: ' + tooltipItems[0].label;
                        },
                        label: function(context) {
                            const topic = context.dataset.label;
                            const heatIndex = context.parsed.y.toFixed(2);
                            return `${topic}: ${heatIndex}`;
                        },
                        afterBody: function(tooltipItems) {
                            // 显示额外信息
                            const month = tooltipItems[0].label;
                            const topic = tooltipItems[0].dataset.label;
                            const topicData = data.find(t => t.topic === topic);

                            if (topicData && topicData.monthlyQuestionCount && topicData.monthlyQuestionCount[month]) {
                                return [
                                    '',
                                    `问题数: ${topicData.monthlyQuestionCount[month]}`,
                                    `平均分: ${topicData.monthlyAvgScore[month]?.toFixed(1) || 0}`,
                                    `答案数: ${topicData.monthlyAnswerCount[month] || 0}`
                                ];
                            }
                            return [];
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    title: {
                        display: true,
                        text: '相对热度指数（0-100）',
                        font: { size: 14, weight: 'bold' }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        callback: function(value) {
                            return value.toFixed(0);
                        }
                    }
                },
                x: {
                    title: {
                        display: true,
                        text: '月份',
                        font: { size: 14, weight: 'bold' }
                    },
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

// 🔥 修改后：表格中添加热度指数列
function renderTrendsTable(data) {
    // 按整体热度排序
    const sortedData = [...data].sort((a, b) =>
        (b.overallHeatIndex || 0) - (a.overallHeatIndex || 0)
    );

    let html = `
        <table class="table table-striped table-hover">
            <thead>
                <tr>
                    <th>主题</th>
                    <th>整体热度</th>
                    <th>总问题数</th>
                    <th>总答案数</th>
                    <th>平均分数</th>
                    <th>平均浏览量</th>
                    <th>采纳率</th>
                    <th>趋势</th>
                </tr>
            </thead>
            <tbody>
    `;

    sortedData.forEach(topic => {
        const trend = topic.overallTrend || 0;
        const heatIndex = topic.overallHeatIndex || 0;

        let trendBadge;
        if (trend > 0.5) {
            trendBadge = `<span class="badge bg-success">📈 ${trend.toFixed(2)}</span>`;
        } else if (trend > 0) {
            trendBadge = `<span class="badge bg-info">↗ ${trend.toFixed(2)}</span>`;
        } else if (trend > -0.5) {
            trendBadge = `<span class="badge bg-secondary">→ ${trend.toFixed(2)}</span>`;
        } else {
            trendBadge = `<span class="badge bg-danger">📉 ${trend.toFixed(2)}</span>`;
        }

        // 热度颜色
        let heatClass = 'text-muted';
        if (heatIndex >= 75) heatClass = 'text-danger fw-bold';
        else if (heatIndex >= 50) heatClass = 'text-warning fw-bold';
        else if (heatIndex >= 25) heatClass = 'text-info';

        html += `
            <tr>
                <td><strong>${topic.topic}</strong></td>
                <td class="${heatClass}">
                    <span class="heat-badge">${heatIndex.toFixed(1)}</span>
                </td>
                <td>${topic.totalQuestions || 0}</td>
                <td>${topic.totalAnswers || 0}</td>
                <td>${(topic.avgScore || 0).toFixed(1)}</td>
                <td>${Math.round(topic.avgViewCount || 0).toLocaleString()}</td>
                <td>${(topic.acceptedAnswerRate || 0).toFixed(1)}%</td>
                <td>${trendBadge}</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    document.getElementById('trendsTable').innerHTML = html;
}

// ==================== 问题2: 主题共现 ====================
function loadCoOccurrence() {
    const topN = document.getElementById('cooccurrenceTopN').value;
    fetch(`/api/cooccurrence?topN=${topN}`)
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderCoOccurrenceChart(result.data);
                renderCoOccurrenceTable(result.data);
            }
        })
        .catch(error => console.error('加载共现数据失败:', error));
}

function renderCoOccurrenceChart(data) {
    const ctx = document.getElementById('cooccurrenceChart').getContext('2d');
    if (cooccurrenceChart) {
        cooccurrenceChart.destroy();
    }

    const labels = data.map(item => `${item.topic1} & ${item.topic2}`);
    const values = data.map(item => item.frequency);

    cooccurrenceChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: '共现频率',
                data: values,
                backgroundColor: COLORS.slice(0, data.length),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            indexAxis: 'y',
            plugins: {
                legend: { display: false },
                title: { display: true, text: 'Top N 主题共现对' }
            },
            scales: {
                x: { beginAtZero: true, title: { display: true, text: '共现次数' } }
            }
        }
    });
}

function renderCoOccurrenceTable(data) {
    let html = `
        <table class="table table-striped table-hover">
            <thead><tr><th>#</th><th>主题1</th><th>主题2</th><th>共现次数</th><th>占比</th></tr></thead>
            <tbody>
    `;

    data.forEach((item, index) => {
        html += `
            <tr>
                <td>${index + 1}</td>
                <td><span class="badge bg-primary">${item.topic1}</span></td>
                <td><span class="badge bg-info">${item.topic2}</span></td>
                <td>${item.frequency}</td>
                <td>${item.percentage}%</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    document.getElementById('cooccurrenceTable').innerHTML = html;
}

// ==================== 问题3: 多线程陷阱 ====================
function loadPitfalls() {
    const topN = document.getElementById('pitfallTopN').value;
    fetch(`/api/multithreading/pitfalls?topN=${topN}`)
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderPitfallCharts(result.data);
                renderPitfallDetails(result.data);
            }
        })
        .catch(error => console.error('加载陷阱数据失败:', error));
}

function renderPitfallCharts(data) {
    // 饼图
    const pieCtx = document.getElementById('pitfallPieChart').getContext('2d');
    if (pitfallPieChart) pitfallPieChart.destroy();

    pitfallPieChart = new Chart(pieCtx, {
        type: 'doughnut',
        data: {
            labels: data.map(item => item.pitfallCategory),
            datasets: [{
                data: data.map(item => item.occurrenceCount),
                backgroundColor: COLORS.slice(0, data.length),
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'right', labels: { boxWidth: 12, font: { size: 11 } } }
            }
        }
    });

    // 柱状图
    const barCtx = document.getElementById('pitfallBarChart').getContext('2d');
    if (pitfallBarChart) pitfallBarChart.destroy();

    pitfallBarChart = new Chart(barCtx, {
        type: 'bar',
        data: {
            labels: data.map(item => item.pitfallCategory),
            datasets: [{
                label: '出现次数',
                data: data.map(item => item.occurrenceCount),
                backgroundColor: COLORS.slice(0, data.length)
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { maxRotation: 45, minRotation: 45, font: { size: 10 } } }
            }
        }
    });
}

function renderPitfallDetails(data) {
    let html = '<div class="row">';

    data.forEach((item, index) => {
        const keywords = item.keywords ? item.keywords.map(k => `<span class="keyword-badge">${k}</span>`).join('') : '';
        html += `
            <div class="col-md-6 mb-3">
                <div class="pitfall-card" style="background: linear-gradient(135deg, ${COLORS[index % COLORS.length]} 0%, ${COLORS[(index + 1) % COLORS.length]} 100%);">
                    <h6>${item.pitfallCategory}</h6>
                    <p class="mb-1"><small>${item.description || ''}</small></p>
                    <p class="mb-1">出现次数: <strong>${item.occurrenceCount}</strong> (${item.percentage}%)</p>
                    <div class="keywords">${keywords}</div>
                </div>
            </div>
        `;
    });

    html += '</div>';
    document.getElementById('pitfallDetails').innerHTML = html;
}

function loadExceptions() {
    fetch('/api/multithreading/exceptions')
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderExceptionChart(result.data);
            }
        })
        .catch(error => console.error('加载异常数据失败:', error));
}

function renderExceptionChart(data) {
    const ctx = document.getElementById('exceptionChart').getContext('2d');
    if (exceptionChart) {
        exceptionChart.destroy();
    }

    const labels = Object.keys(data);
    const values = Object.values(data);

    exceptionChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: '出现次数',
                data: values,
                backgroundColor: COLORS.slice(0, labels.length)
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { title: { display: true, text: '多线程相关异常分布' } },
            scales: {
                x: { ticks: { maxRotation: 45, minRotation: 45 } },
                y: { beginAtZero: true }
            }
        }
    });
}

// ==================== 问题4: 可解决性分析 ====================
function loadSolvability() {
    fetch('/api/solvability/factors')
        .then(response => response.json())
        .then(result => {
            console.log('可解决性数据:', result);
            if (result.success && result.data) {
                renderSolvabilityPieChart(result.data);
                renderFactorsChart(result.data);
                renderComparisonChart(result.data);
                renderSolvabilityTable(result.data);
            }
        })
        .catch(error => console.error('加载可解决性数据失败:', error));
}

function renderSolvabilityPieChart(data) {
    const ctx = document.getElementById('solvabilityPieChart').getContext('2d');
    if (solvabilityPieChart) solvabilityPieChart.destroy();

    const counts = data.questionCounts || { solvable: 0, hardToSolve: 0 };

    solvabilityPieChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: ['可解决', '难解决'],
            datasets: [{
                data: [counts.solvable || 0, counts.hardToSolve || 0],
                backgroundColor: ['#28A745', '#DC3545']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

function renderFactorsChart(data) {
    const ctx = document.getElementById('factorsChart').getContext('2d');
    if (factorsChart) factorsChart.destroy();

    const factors = [
        { key: 'titleLength', label: '标题长度' },
        { key: 'codeSnippets', label: '代码片段' },
        { key: 'tagCount', label: '标签数量' }
    ];

    const labels = factors.map(f => f.label);
    const solvableData = factors.map(f => (data[f.key] && data[f.key].solvable) ? data[f.key].solvable : 0);
    const hardToSolveData = factors.map(f => (data[f.key] && data[f.key].hardToSolve) ? data[f.key].hardToSolve : 0);

    factorsChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                { label: '可解决', data: solvableData, backgroundColor: '#28A745' },
                { label: '难解决', data: hardToSolveData, backgroundColor: '#DC3545' }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { title: { display: true, text: '特征因素对比' } },
            scales: { y: { beginAtZero: true } }
        }
    });
}

function renderComparisonChart(data) {
    const ctx = document.getElementById('comparisonChart');
    if (!ctx) {
        console.error('找不到 comparisonChart canvas');
        return;
    }

    if (comparisonChart) comparisonChart.destroy();

    const solvableBody = (data.bodyLength && data.bodyLength.solvable) ? data.bodyLength.solvable : 0;
    const hardToSolveBody = (data.bodyLength && data.bodyLength.hardToSolve) ? data.bodyLength.hardToSolve : 0;
    const solvableRep = (data.userReputation && data.userReputation.solvable) ? data.userReputation.solvable : 0;
    const hardToSolveRep = (data.userReputation && data.userReputation.hardToSolve) ? data.userReputation.hardToSolve : 0;

    comparisonChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['正文长度', '用户声望'],
            datasets: [
                { label: '可解决', data: [solvableBody, solvableRep], backgroundColor: '#28A745' },
                { label: '难解决', data: [hardToSolveBody, hardToSolveRep], backgroundColor: '#DC3545' }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { title: { display: true, text: '正文长度与用户声望对比' } },
            scales: { y: { beginAtZero: true } }
        }
    });
}

function renderSolvabilityTable(data) {
    const factorMap = {
        'titleLength': '平均标题长度',
        'bodyLength': '平均正文长度',
        'codeSnippets': '平均代码片段数',
        'userReputation': '平均用户声望',
        'tagCount': '平均标签数量'
    };

    let html = `
        <table class="table table-bordered">
            <thead>
                <tr>
                    <th>因素</th>
                    <th class="text-success">可解决问题</th>
                    <th class="text-danger">难解决问题</th>
                    <th>差异</th>
                </tr>
            </thead>
            <tbody>
    `;

    for (const [key, label] of Object.entries(factorMap)) {
        const solvable = (data[key] && data[key].solvable !== undefined) ? data[key].solvable : 0;
        const hardToSolve = (data[key] && data[key].hardToSolve !== undefined) ? data[key].hardToSolve : 0;
        const diff = (solvable - hardToSolve).toFixed(2);
        const diffClass = parseFloat(diff) > 0 ? 'text-success' : (parseFloat(diff) < 0 ? 'text-danger' : '');

        html += `
            <tr>
                <td><strong>${label}</strong></td>
                <td>${solvable.toFixed(2)}</td>
                <td>${hardToSolve.toFixed(2)}</td>
                <td class="${diffClass}">${parseFloat(diff) > 0 ? '+' : ''}${diff}</td>
            </tr>
        `;
    }

    html += '</tbody></table>';
    document.getElementById('solvabilityDetails').innerHTML = html;
}

function loadTiming() {
    fetch('/api/solvability/timing')
        .then(response => response.json())
        .then(result => {
            if (result.success && result.data) {
                renderTimingChart(result.data);
            }
        })
        .catch(error => console.error('加载时间分布失败:', error));
}

function renderTimingChart(data) {
    const ctx = document.getElementById('timingChart').getContext('2d');
    if (timingChart) timingChart.destroy();

    const hours = Array.from({length: 24}, (_, i) => i);
    const solvableData = hours.map(h => data.solvableHourDistribution ? (data.solvableHourDistribution[h] || 0) : 0);
    const hardToSolveData = hours.map(h => data.hardToSolveHourDistribution ? (data.hardToSolveHourDistribution[h] || 0) : 0);

    timingChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: hours.map(h => `${h}:00`),
            datasets: [
                {
                    label: '可解决问题',
                    data: solvableData,
                    borderColor: '#28A745',
                    backgroundColor: 'rgba(40, 167, 69, 0.1)',
                    tension: 0.4,
                    fill: true
                },
                {
                    label: '难解决问题',
                    data: hardToSolveData,
                    borderColor: '#DC3545',
                    backgroundColor: 'rgba(220, 53, 69, 0.1)',
                    tension: 0.4,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { title: { display: true, text: '按发布小时分布对比' } },
            scales: {
                y: { beginAtZero: true, title: { display: true, text: '问题数量' } },
                x: { title: { display: true, text: '发布时间（小时）' } }
            }
        }
    });
}