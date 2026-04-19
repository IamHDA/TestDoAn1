import os
import re
import csv

TEST_DIR = r"D:\Documents\SubDocuments\K8_Quality-Assurrance\Project-class-system\TestDoAn1\src\test\java\com\vn\backend"
OUTPUT_FILE = "Unit_Testing_Report.csv"

test_cases = []

for filename in os.listdir(TEST_DIR):
    if filename.endswith("Test.java") and not filename.endswith(".ignored"):
        filepath = os.path.join(TEST_DIR, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            
            # Find all test methods
            matches = re.finditer(r'@Test\s+(?:@DisplayName\(\s*"([^"]+)"\s*\))?\s*(?:@[^\n]+)*\s*void\s+([a-zA-Z0-9_]+)\(', content)
            class_name = filename.replace(".java", "")
            
            for index, match in enumerate(matches, 1):
                display_name = match.group(1)
                method_name = match.group(2)
                
                objective = display_name if display_name else method_name
                
                test_cases.append({
                    "Test Case ID": f"TC_{class_name.upper().replace('SERVICEIMPLTEST', '')}_{index:02d}",
                    "Tên Tệp/Lớp": class_name,
                    "Test Objective": objective,
                    "Input": "Tham khảo mã nguồn Mock",
                    "Expected Output": "Các asssertions Pass",
                    "Notes": "Dùng Mockito mock repository"
                })

# Write to CSV
with open(OUTPUT_FILE, 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.writer(f)
    # Header
    writer.writerow(["Test Case ID", "Tên Tệp/Lớp", "Test Objective", "Input", "Expected Output", "Notes"])
    for tc in test_cases:
        writer.writerow([tc["Test Case ID"], tc["Tên Tệp/Lớp"], tc["Test Objective"], tc["Input"], tc["Expected Output"], tc["Notes"]])

print(f"✅ Đã tạo file CSV thành công: {OUTPUT_FILE} với {len(test_cases)} test cases.")
print("Bạn có thể mở file này bằng Excel và lưu lại thành file .xlsx để làm báo cáo.")
