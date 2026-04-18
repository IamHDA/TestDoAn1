"""
Generate HTML Report từ JSON report theo chuẩn JMeter
"""
import json
import sys
import io
import glob
from datetime import datetime

# Fix encoding for Windows
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def generate_html_report(json_file: str):
    """Generate HTML report từ JSON file"""
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    test_info = data["test_info"]
    summary = data["summary"]
    api_statistics = data["api_statistics"]
    
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Performance Test Report</title>
    <style>
        body {{
            font-family: Arial, Helvetica, sans-serif;
            font-size: 12px;
            margin: 0;
            padding: 20px;
            background-color: #ffffff;
            color: #333333;
        }}
        .report-header {{
            border-bottom: 2px solid #333333;
            padding-bottom: 10px;
            margin-bottom: 20px;
        }}
        .report-header h1 {{
            font-size: 18px;
            font-weight: bold;
            margin: 0;
            color: #000000;
        }}
        .report-header p {{
            font-size: 11px;
            color: #666666;
            margin: 5px 0 0 0;
        }}
        .section {{
            margin-bottom: 30px;
        }}
        .section-title {{
            font-size: 14px;
            font-weight: bold;
            color: #000000;
            border-bottom: 1px solid #cccccc;
            padding-bottom: 5px;
            margin-bottom: 15px;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
            font-size: 11px;
        }}
        table th {{
            background-color: #e6e6e6;
            border: 1px solid #cccccc;
            padding: 8px;
            text-align: left;
            font-weight: bold;
            color: #000000;
        }}
        table td {{
            border: 1px solid #cccccc;
            padding: 8px;
            background-color: #ffffff;
        }}
        table tr:nth-child(even) td {{
            background-color: #f9f9f9;
        }}
        .metric-table {{
            margin-top: 10px;
        }}
        .metric-table th {{
            width: 150px;
        }}
        .endpoint-section {{
            margin-bottom: 25px;
            border: 1px solid #cccccc;
            padding: 15px;
            background-color: #fafafa;
        }}
        .endpoint-title {{
            font-size: 13px;
            font-weight: bold;
            color: #000000;
            margin-bottom: 10px;
            padding-bottom: 5px;
            border-bottom: 1px solid #dddddd;
        }}
        .metric-label {{
            font-weight: bold;
            color: #333333;
        }}
        .value-success {{
            color: #006600;
        }}
        .value-error {{
            color: #cc0000;
        }}
        .value-number {{
            text-align: right;
            font-family: 'Courier New', monospace;
        }}
        .error-list {{
            margin-top: 10px;
            padding-left: 20px;
        }}
        .error-item {{
            font-size: 10px;
            color: #666666;
            margin: 3px 0;
        }}
        .summary-table {{
            width: auto;
        }}
        .summary-table td:first-child {{
            font-weight: bold;
            width: 200px;
        }}
    </style>
</head>
<body>
    <div class="report-header">
        <h1>Performance Test Report</h1>
        <p>Live Exam Feature - {test_info['session_exam_id']} | Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
    </div>
    
    <div class="section">
        <div class="section-title">Test Summary</div>
        <table class="summary-table">
            <tr>
                <td>Total Users</td>
                <td class="value-number">{test_info['num_students']}</td>
            </tr>
            <tr>
                <td>Test Duration</td>
                <td class="value-number">{test_info['total_time_seconds']:.2f} seconds</td>
            </tr>
            <tr>
                <td>Successful Flows</td>
                <td class="value-number value-success">{summary['successful_flows']}</td>
            </tr>
            <tr>
                <td>Failed Flows</td>
                <td class="value-number value-error">{summary['failed_flows']}</td>
            </tr>
            <tr>
                <td>Success Rate</td>
                <td class="value-number">{summary['success_rate']:.2f}%</td>
            </tr>
            <tr>
                <td>Throughput (users/s)</td>
                <td class="value-number">{summary['overall_throughput']:.2f}</td>
            </tr>
            <tr>
                <td>Total Requests</td>
                <td class="value-number">{summary['total_requests']}</td>
            </tr>
            <tr>
                <td>Total Failures</td>
                <td class="value-number value-error">{summary['total_failures']}</td>
            </tr>
            <tr>
                <td>Error Rate</td>
                <td class="value-number value-error">{summary['overall_error_rate']:.2f}%</td>
            </tr>
            <tr>
                <td>Throughput (RPS)</td>
                <td class="value-number">{summary['overall_throughput_rps']:.2f}</td>
            </tr>
        </table>
    </div>
    
    <div class="section">
        <div class="section-title">Test Configuration</div>
        <table class="summary-table">
            <tr>
                <td>Base URL</td>
                <td>{test_info['base_url']}</td>
            </tr>
            <tr>
                <td>Session Exam ID</td>
                <td>{test_info['session_exam_id']}</td>
            </tr>
            <tr>
                <td>Test Start</td>
                <td>{test_info['test_start']}</td>
            </tr>
            <tr>
                <td>Test End</td>
                <td>{test_info['test_end']}</td>
            </tr>
        </table>
    </div>
    
    <div class="section">
        <div class="section-title">API Endpoint Statistics</div>
"""
    
    for api_name, stats in api_statistics.items():
        if stats['total_requests'] == 0:
            continue
            
        html_content += f"""
        <div class="endpoint-section">
            <div class="endpoint-title">{stats['endpoint']}</div>
            
            <table class="metric-table">
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                </tr>
                <tr>
                    <td class="metric-label">Total Requests</td>
                    <td class="value-number">{stats['total_requests']}</td>
                </tr>
                <tr>
                    <td class="metric-label">Success</td>
                    <td class="value-number value-success">{stats['success_count']} ({100 - stats['error_rate']:.2f}%)</td>
                </tr>
                <tr>
                    <td class="metric-label">Failures</td>
                    <td class="value-number value-error">{stats['failure_count']} ({stats['error_rate']:.2f}%)</td>
                </tr>
                <tr>
                    <td class="metric-label">Throughput</td>
                    <td class="value-number">{stats['throughput']:.2f} req/s</td>
                </tr>
            </table>
            
            <table class="metric-table">
                <tr>
                    <th colspan="2">Response Time Statistics (ms)</th>
                </tr>
                <tr>
                    <td class="metric-label">Average</td>
                    <td class="value-number">{stats['avg_response_time']:.2f}</td>
                </tr>
                <tr>
                    <td class="metric-label">Min</td>
                    <td class="value-number">{stats['min_response_time']:.2f}</td>
                </tr>
                <tr>
                    <td class="metric-label">Max</td>
                    <td class="value-number">{stats['max_response_time']:.2f}</td>
                </tr>
                <tr>
                    <td class="metric-label">Median</td>
                    <td class="value-number">{stats['median_response_time']:.2f}</td>
                </tr>
                <tr>
                    <td class="metric-label">Std Deviation</td>
                    <td class="value-number">{stats['std_deviation']:.2f}</td>
                </tr>
            </table>
            
            <table class="metric-table">
                <tr>
                    <th colspan="2">Percentiles (ms)</th>
                </tr>
"""
        for p, value in stats['percentiles'].items():
            html_content += f"""
                <tr>
                    <td class="metric-label">P{p}</td>
                    <td class="value-number">{value:.2f}</td>
                </tr>
"""
        
        html_content += """
            </table>
"""
        
        if stats.get('errors'):
            html_content += f"""
            <div style="margin-top: 10px;">
                <div class="metric-label" style="margin-bottom: 5px;">Top Errors:</div>
                <div class="error-list">
"""
            error_counts = {}
            for error in stats['errors']:
                error_key = f"Status {error.get('status_code', 'Unknown')}: {error.get('error', 'Unknown')[:80]}"
                error_counts[error_key] = error_counts.get(error_key, 0) + 1
            
            for error_msg, count in list(error_counts.items())[:5]:
                html_content += f"""
                    <div class="error-item">• {error_msg} ({count} times)</div>
"""
            html_content += """
                </div>
            </div>
"""
        
        html_content += """
        </div>
"""
    
    html_content += """
    </div>
</body>
</html>
"""
    
    html_file = json_file.replace('.json', '_report.html')
    with open(html_file, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"HTML report đã được tạo: {html_file}")

def main():
    if len(sys.argv) > 1:
        json_file = sys.argv[1]
    else:
        # Tìm file mới nhất
        files = glob.glob("performance_test_live_exam_jmeter_*.json")
        if not files:
            print("Không tìm thấy file JSON report!")
            return
        json_file = max(files, key=lambda f: f)
        print(f"Sử dụng file: {json_file}\n")
    
    generate_html_report(json_file)

if __name__ == "__main__":
    main()

