"""
Performance Test Script for Live Exam Feature - JMeter Style
Tests students joining, downloading, saving answers, and submitting exam concurrently
Với báo cáo chuẩn performance test theo format JMeter
"""
import asyncio
import aiohttp
import time
import json
import sys
import io
from datetime import datetime
from typing import List, Dict, Optional, Tuple
import statistics
import random
from collections import defaultdict

# Fix encoding for Windows
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# Configuration
BASE_URL = "http://139.162.31.27:8081"
LOGIN_ENDPOINT = f"{BASE_URL}/api/auth/login"
NUM_STUDENTS = 300
PASSWORD = "123456"
SESSION_EXAM_ID = 44

class JMeterStyleMetrics:
    """Class để tính toán các metrics theo chuẩn JMeter"""
    
    @staticmethod
    def calculate_percentiles(times: List[float], percentiles: List[int] = [50, 90, 95, 99]) -> Dict[int, float]:
        """Tính toán percentiles"""
        if not times:
            return {p: 0.0 for p in percentiles}
        
        sorted_times = sorted(times)
        result = {}
        for p in percentiles:
            index = int((p / 100.0) * len(sorted_times))
            if index >= len(sorted_times):
                index = len(sorted_times) - 1
            result[p] = sorted_times[index]
        return result
    
    @staticmethod
    def calculate_throughput(total_requests: int, total_time_seconds: float) -> float:
        """Tính throughput (requests per second)"""
        if total_time_seconds <= 0:
            return 0.0
        return total_requests / total_time_seconds
    
    @staticmethod
    def calculate_error_rate(total_requests: int, failed_requests: int) -> float:
        """Tính error rate (%)"""
        if total_requests == 0:
            return 0.0
        return (failed_requests / total_requests) * 100

class APIMetric:
    """Class để lưu metrics cho từng API endpoint"""
    def __init__(self, endpoint_name: str):
        self.endpoint_name = endpoint_name
        self.response_times: List[float] = []
        self.success_count = 0
        self.failure_count = 0
        self.errors: List[Dict] = []
        self.start_time = None
        self.end_time = None
    
    def add_result(self, response_time_ms: float, success: bool, status_code: int = None, error: str = None):
        """Thêm kết quả một request"""
        self.response_times.append(response_time_ms)
        if success:
            self.success_count += 1
        else:
            self.failure_count += 1
            if error:
                self.errors.append({
                    "status_code": status_code,
                    "error": error,
                    "response_time_ms": response_time_ms
                })
    
    def get_statistics(self) -> Dict:
        """Lấy thống kê cho API này"""
        if not self.response_times:
            return {
                "total_requests": 0,
                "success_count": 0,
                "failure_count": 0,
                "error_rate": 0.0,
                "throughput": 0.0,
                "avg_response_time": 0.0,
                "min_response_time": 0.0,
                "max_response_time": 0.0,
                "median_response_time": 0.0,
                "percentiles": {}
            }
        
        total_requests = len(self.response_times)
        total_time = (self.end_time - self.start_time) if self.start_time and self.end_time else 0
        
        return {
            "endpoint": self.endpoint_name,
            "total_requests": total_requests,
            "success_count": self.success_count,
            "failure_count": self.failure_count,
            "error_rate": JMeterStyleMetrics.calculate_error_rate(total_requests, self.failure_count),
            "throughput": JMeterStyleMetrics.calculate_throughput(total_requests, total_time) if total_time > 0 else 0.0,
            "avg_response_time": statistics.mean(self.response_times),
            "min_response_time": min(self.response_times),
            "max_response_time": max(self.response_times),
            "median_response_time": statistics.median(self.response_times),
            "std_deviation": statistics.stdev(self.response_times) if len(self.response_times) > 1 else 0.0,
            "percentiles": JMeterStyleMetrics.calculate_percentiles(self.response_times),
            "errors": self.errors[:10]  # Top 10 errors
        }

class LiveExamTestJMeter:
    def __init__(self):
        self.results: List[Dict] = []
        self.start_time = None
        self.end_time = None
        self.api_metrics: Dict[str, APIMetric] = {}
        
        # Khởi tạo metrics cho từng API
        self.api_metrics["login"] = APIMetric("POST /api/auth/login")
        self.api_metrics["join"] = APIMetric(f"POST /api/session-exams/{SESSION_EXAM_ID}/join")
        self.api_metrics["download"] = APIMetric(f"GET /api/session-exams/{SESSION_EXAM_ID}/download")
        self.api_metrics["save_answers"] = APIMetric(f"POST /api/session-exams/{SESSION_EXAM_ID}/save")
        self.api_metrics["submit"] = APIMetric(f"POST /api/session-exams/{SESSION_EXAM_ID}/submit")
        
    async def login(self, session: aiohttp.ClientSession, username: str) -> Tuple[Optional[str], float, bool, Optional[str]]:
        """Login và lấy JWT token. Returns: (token, response_time_ms, success, error)"""
        login_data = {"username": username, "password": PASSWORD}
        start_time = time.time()
        
        try:
            async with session.post(
                LOGIN_ENDPOINT,
                json=login_data,
                headers={"Content-Type": "application/json"},
                timeout=aiohttp.ClientTimeout(total=30)
            ) as response:
                response_time = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    data = await response.json()
                    token_data = data.get("data", {})
                    token = token_data.get("token") or token_data.get("accessToken")
                    
                    if token:
                        self.api_metrics["login"].add_result(response_time, True, response.status)
                        return (token, response_time, True, None)
                    else:
                        error_msg = f"Token not found in response"
                        self.api_metrics["login"].add_result(response_time, False, response.status, error_msg)
                        return (None, response_time, False, error_msg)
                else:
                    response_text = await response.text()
                    error_msg = response_text[:200]
                    self.api_metrics["login"].add_result(response_time, False, response.status, error_msg)
                    return (None, response_time, False, error_msg)
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            error_msg = str(e)
            self.api_metrics["login"].add_result(response_time, False, 0, error_msg)
            return (None, response_time, False, error_msg)
    
    async def join_exam(self, session: aiohttp.ClientSession, jwt_token: str, session_exam_id: int) -> Tuple[Optional[str], float, bool, Optional[str]]:
        """Join vào session exam. Returns: (sessionToken, response_time_ms, success, error)"""
        if not jwt_token:
            return (None, 0, False, "JWT token is empty")
        
        url = f"{BASE_URL}/api/session-exams/{session_exam_id}/join"
        headers = {
            "Authorization": f"Bearer {jwt_token}".strip(),
            "Content-Type": "application/json"
        }
        start_time = time.time()
        
        try:
            async with session.post(url, headers=headers, timeout=aiohttp.ClientTimeout(total=30)) as response:
                response_time = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    data = await response.json()
                    session_token = data.get("data", {}).get("sessionToken")
                    if session_token:
                        self.api_metrics["join"].add_result(response_time, True, response.status)
                        return (session_token, response_time, True, None)
                    else:
                        error_msg = "SessionToken not found in response"
                        self.api_metrics["join"].add_result(response_time, False, response.status, error_msg)
                        return (None, response_time, False, error_msg)
                else:
                    response_text = await response.text()
                    error_msg = response_text[:200]
                    self.api_metrics["join"].add_result(response_time, False, response.status, error_msg)
                    return (None, response_time, False, error_msg)
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            error_msg = str(e)
            self.api_metrics["join"].add_result(response_time, False, 0, error_msg)
            return (None, response_time, False, error_msg)
    
    async def download_exam(self, session: aiohttp.ClientSession, jwt_token: str, 
                           session_exam_id: int, session_token: str) -> Tuple[Optional[List], float, bool, Optional[str]]:
        """Download đề thi. Returns: (questions, response_time_ms, success, error)"""
        url = f"{BASE_URL}/api/session-exams/{session_exam_id}/download"
        headers = {
            "Authorization": f"Bearer {jwt_token}".strip(),
            "X-Session-Token": session_token,
            "Content-Type": "application/json"
        }
        start_time = time.time()
        
        try:
            async with session.get(url, headers=headers, timeout=aiohttp.ClientTimeout(total=30)) as response:
                response_time = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    data = await response.json()
                    questions = data.get("data", {}).get("questions", [])
                    self.api_metrics["download"].add_result(response_time, True, response.status)
                    return (questions, response_time, True, None)
                else:
                    response_text = await response.text()
                    error_msg = response_text[:200]
                    self.api_metrics["download"].add_result(response_time, False, response.status, error_msg)
                    return (None, response_time, False, error_msg)
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            error_msg = str(e)
            self.api_metrics["download"].add_result(response_time, False, 0, error_msg)
            return (None, response_time, False, error_msg)
    
    async def save_answers(self, session: aiohttp.ClientSession, jwt_token: str,
                          session_exam_id: int, session_token: str, questions: List[Dict]) -> Tuple[bool, float, Optional[str]]:
        """Lưu câu trả lời. Returns: (success, response_time_ms, error)"""
        url = f"{BASE_URL}/api/session-exams/{session_exam_id}/save"
        headers = {
            "Authorization": f"Bearer {jwt_token}".strip(),
            "X-Session-Token": session_token,
            "Content-Type": "application/json"
        }
        
        # Tạo answers giả lập
        answers = []
        for question in questions[:10]:
            question_id = question.get("id")
            answer_options = question.get("answers", [])
            if answer_options:
                selected_count = random.randint(1, min(2, len(answer_options)))
                selected_ids = [opt.get("id") for opt in random.sample(answer_options, selected_count)]
                answers.append({
                    "questionSnapshotId": question_id,
                    "selectedAnswerIds": selected_ids
                })
        
        request_data = {"answers": answers}
        start_time = time.time()
        
        try:
            async with session.post(url, json=request_data, headers=headers, timeout=aiohttp.ClientTimeout(total=30)) as response:
                response_time = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    self.api_metrics["save_answers"].add_result(response_time, True, response.status)
                    return (True, response_time, None)
                else:
                    response_text = await response.text()
                    error_msg = response_text[:200]
                    self.api_metrics["save_answers"].add_result(response_time, False, response.status, error_msg)
                    return (False, response_time, error_msg)
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            error_msg = str(e)
            self.api_metrics["save_answers"].add_result(response_time, False, 0, error_msg)
            return (False, response_time, error_msg)
    
    async def submit_exam(self, session: aiohttp.ClientSession, jwt_token: str,
                         session_exam_id: int, session_token: str, questions: List[Dict]) -> Tuple[bool, float, Optional[float], Optional[str]]:
        """Nộp bài thi. Returns: (success, response_time_ms, score, error)"""
        url = f"{BASE_URL}/api/session-exams/{session_exam_id}/submit"
        headers = {
            "Authorization": f"Bearer {jwt_token}".strip(),
            "X-Session-Token": session_token,
            "Content-Type": "application/json"
        }
        
        # Tạo answers để submit
        answers = []
        for question in questions:
            question_id = question.get("id")
            answer_options = question.get("answers", [])
            if answer_options:
                selected_count = random.randint(1, min(2, len(answer_options)))
                selected_ids = [opt.get("id") for opt in random.sample(answer_options, selected_count)]
                answers.append({
                    "questionSnapshotId": question_id,
                    "selectedAnswerIds": selected_ids
                })
        
        request_data = {"answers": answers}
        start_time = time.time()
        
        try:
            async with session.post(url, json=request_data, headers=headers, timeout=aiohttp.ClientTimeout(total=30)) as response:
                response_time = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    data = await response.json()
                    score = data.get("data", {}).get("score", 0)
                    self.api_metrics["submit"].add_result(response_time, True, response.status)
                    return (True, response_time, score, None)
                else:
                    response_text = await response.text()
                    error_msg = response_text[:200]
                    self.api_metrics["submit"].add_result(response_time, False, response.status, error_msg)
                    return (False, response_time, None, error_msg)
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            error_msg = str(e)
            self.api_metrics["submit"].add_result(response_time, False, 0, error_msg)
            return (False, response_time, None, error_msg)
    
    async def test_student_exam_flow(self, session: aiohttp.ClientSession, username: str, 
                                     session_exam_id: int) -> Dict:
        """Test toàn bộ flow thi của một sinh viên"""
        student_result = {
            "username": username,
            "timestamp": datetime.now().isoformat(),
            "steps": {},
            "overall_success": False,
            "total_time_ms": 0,
            "errors": []
        }
        
        flow_start = time.time()
        
        # Step 1: Login
        token, login_time, login_success, login_error = await self.login(session, username)
        student_result["steps"]["login"] = {
            "success": login_success,
            "response_time_ms": round(login_time, 2)
        }
        
        if not login_success or not token:
            student_result["errors"].append(f"Login failed: {login_error}")
            student_result["total_time_ms"] = (time.time() - flow_start) * 1000
            return student_result
        
        # Step 2: Join exam
        session_token, join_time, join_success, join_error = await self.join_exam(session, token, session_exam_id)
        student_result["steps"]["join"] = {
            "success": join_success,
            "response_time_ms": round(join_time, 2)
        }
        
        if not join_success:
            student_result["errors"].append(f"Join failed: {join_error}")
            student_result["total_time_ms"] = (time.time() - flow_start) * 1000
            return student_result
        
        # Step 3: Download exam
        questions, download_time, download_success, download_error = await self.download_exam(session, token, session_exam_id, session_token)
        student_result["steps"]["download"] = {
            "success": download_success,
            "response_time_ms": round(download_time, 2),
            "questionCount": len(questions) if questions else 0
        }
        
        if not download_success:
            student_result["errors"].append(f"Download failed: {download_error}")
            student_result["total_time_ms"] = (time.time() - flow_start) * 1000
            return student_result
        
        # Step 4: Save answers
        save_success, save_time, save_error = await self.save_answers(session, token, session_exam_id, session_token, questions)
        student_result["steps"]["save_answers"] = {
            "success": save_success,
            "response_time_ms": round(save_time, 2)
        }
        
        if not save_success:
            student_result["errors"].append(f"Save answers failed: {save_error}")
        
        # Step 5: Submit exam
        submit_success, submit_time, score, submit_error = await self.submit_exam(session, token, session_exam_id, session_token, questions)
        student_result["steps"]["submit"] = {
            "success": submit_success,
            "response_time_ms": round(submit_time, 2),
            "score": score
        }
        
        if not submit_success:
            student_result["errors"].append(f"Submit failed: {submit_error}")
        
        student_result["total_time_ms"] = round((time.time() - flow_start) * 1000, 2)
        student_result["overall_success"] = all([
            student_result["steps"]["login"].get("success"),
            student_result["steps"]["join"].get("success"),
            student_result["steps"]["download"].get("success"),
            student_result["steps"]["submit"].get("success")
        ])
        
        return student_result
    
    async def run_test(self, session_exam_id: int):
        """Chạy test với nhiều sinh viên đồng thời"""
        print(f" Bắt đầu Performance Test - Live Exam (JMeter Style)")
        print(f" Base URL: {BASE_URL}")
        print(f" Session Exam ID: {session_exam_id}")
        print(f" Số lượng sinh viên: {NUM_STUDENTS}")
        print(f" Password: {PASSWORD}")
        print("=" * 100)
        
        # Set start time cho metrics
        for metric in self.api_metrics.values():
            metric.start_time = time.time()
        
        self.start_time = time.time()
        
        usernames = [f"student{i}" for i in range(1, NUM_STUDENTS + 1)]
        
        connector = aiohttp.TCPConnector(limit=500, limit_per_host=500)
        async with aiohttp.ClientSession(connector=connector) as session:
            tasks = [self.test_student_exam_flow(session, username, session_exam_id) 
                    for username in usernames]
            self.results = await asyncio.gather(*tasks)
        
        self.end_time = time.time()
        
        # Set end time cho metrics
        for metric in self.api_metrics.values():
            metric.end_time = time.time()
    
    def generate_jmeter_style_report(self):
        """Tạo báo cáo theo chuẩn JMeter"""
        total_time = self.end_time - self.start_time
        
        # Tính toán tổng quan
        successful_flows = [r for r in self.results if r["overall_success"]]
        failed_flows = [r for r in self.results if not r["overall_success"]]
        
        success_count = len(successful_flows)
        failure_count = len(failed_flows)
        success_rate = (success_count / NUM_STUDENTS) * 100
        
        # Lấy statistics cho từng API
        api_statistics = {}
        for api_name, metric in self.api_metrics.items():
            api_statistics[api_name] = metric.get_statistics()
        
        # In báo cáo
        print("\n" + "=" * 100)
        print("📊 PERFORMANCE TEST REPORT - JMETER STYLE")
        print("=" * 100)
        
        print(f"\n📋 TEST SUMMARY")
        print("-" * 100)
        print(f"  Test Duration: {total_time:.2f} seconds")
        print(f"  Total Users: {NUM_STUDENTS}")
        print(f"  Successful Flows: {success_count} ({success_rate:.2f}%)")
        print(f"  Failed Flows: {failure_count} ({100 - success_rate:.2f}%)")
        print(f"  Overall Throughput: {NUM_STUDENTS / total_time:.2f} users/second")
        
        print(f"\n📊 API ENDPOINT STATISTICS")
        print("=" * 100)
        
        for api_name, stats in api_statistics.items():
            print(f"\n🔹 {stats['endpoint']}")
            print("-" * 100)
            print(f"  Total Requests: {stats['total_requests']}")
            print(f"  Success: {stats['success_count']} ({100 - stats['error_rate']:.2f}%)")
            print(f"  Failures: {stats['failure_count']} ({stats['error_rate']:.2f}%)")
            print(f"  Throughput: {stats['throughput']:.2f} requests/second")
            print(f"\n  Response Time (ms):")
            print(f"    Average: {stats['avg_response_time']:.2f}")
            print(f"    Min: {stats['min_response_time']:.2f}")
            print(f"    Max: {stats['max_response_time']:.2f}")
            print(f"    Median: {stats['median_response_time']:.2f}")
            print(f"    Std Deviation: {stats['std_deviation']:.2f}")
            print(f"    Percentiles:")
            for p, value in stats['percentiles'].items():
                print(f"      P{p}: {value:.2f} ms")
            
            if stats['errors']:
                print(f"\n  Top Errors:")
                error_counts = defaultdict(int)
                for error in stats['errors']:
                    error_key = f"Status {error.get('status_code', 'Unknown')}: {error.get('error', 'Unknown')[:50]}"
                    error_counts[error_key] += 1
                for error_msg, count in list(error_counts.items())[:5]:
                    print(f"    - {error_msg} ({count} times)")
        
        # Aggregate statistics
        print(f"\n📈 AGGREGATE STATISTICS")
        print("=" * 100)
        
        all_response_times = []
        for api_name, metric in self.api_metrics.items():
            all_response_times.extend(metric.response_times)
        
        if all_response_times:
            print(f"  Overall Response Time:")
            print(f"    Average: {statistics.mean(all_response_times):.2f} ms")
            print(f"    Min: {min(all_response_times):.2f} ms")
            print(f"    Max: {max(all_response_times):.2f} ms")
            print(f"    Median: {statistics.median(all_response_times):.2f} ms")
            
            overall_percentiles = JMeterStyleMetrics.calculate_percentiles(all_response_times)
            print(f"    Percentiles:")
            for p, value in overall_percentiles.items():
                print(f"      P{p}: {value:.2f} ms")
        
        total_requests = sum(stats['total_requests'] for stats in api_statistics.values())
        total_failures = sum(stats['failure_count'] for stats in api_statistics.values())
        overall_error_rate = (total_failures / total_requests * 100) if total_requests > 0 else 0
        
        print(f"\n  Overall Metrics:")
        print(f"    Total Requests: {total_requests}")
        print(f"    Total Failures: {total_failures}")
        print(f"    Overall Error Rate: {overall_error_rate:.2f}%")
        print(f"    Overall Throughput: {total_requests / total_time:.2f} requests/second")
        
        # Lưu báo cáo chi tiết
        report_file = f"performance_test_live_exam_jmeter_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        print(f"\n💡 Để tạo HTML report, chạy:")
        print(f"   python generate_jmeter_html_report.py {report_file}")
        report_data = {
            "test_info": {
                "base_url": BASE_URL,
                "session_exam_id": SESSION_EXAM_ID,
                "num_students": NUM_STUDENTS,
                "test_start": datetime.fromtimestamp(self.start_time).isoformat(),
                "test_end": datetime.fromtimestamp(self.end_time).isoformat(),
                "total_time_seconds": round(total_time, 2)
            },
            "summary": {
                "total_users": NUM_STUDENTS,
                "successful_flows": success_count,
                "failed_flows": failure_count,
                "success_rate": round(success_rate, 2),
                "overall_throughput": round(NUM_STUDENTS / total_time, 2),
                "total_requests": total_requests,
                "total_failures": total_failures,
                "overall_error_rate": round(overall_error_rate, 2),
                "overall_throughput_rps": round(total_requests / total_time, 2)
            },
            "api_statistics": api_statistics,
            "detailed_results": self.results
        }
        
        with open(report_file, "w", encoding="utf-8") as f:
            json.dump(report_data, f, indent=2, ensure_ascii=False)
        
        print(f"\n💾 Báo cáo chi tiết đã được lưu vào: {report_file}")
        print("=" * 100)

async def main():
    """Main function"""
    session_exam_id = SESSION_EXAM_ID
    if len(sys.argv) > 1:
        try:
            session_exam_id = int(sys.argv[1])
        except ValueError:
            print(f"⚠️  Session Exam ID không hợp lệ, sử dụng mặc định: {SESSION_EXAM_ID}")
    
    test = LiveExamTestJMeter()
    await test.run_test(session_exam_id)
    test.generate_jmeter_style_report()

if __name__ == "__main__":
    print("🔧 Performance Test Tool for Live Exam - JMeter Style")
    print("=" * 100)
    print("Usage: python performance_test_live_exam_jmeter_style.py [session_exam_id]")
    print("=" * 100)
    asyncio.run(main())

