import pandas as pd

file_path = r'd:\CC\SQA\Code BE-TEST\TestDoAn1\4 - Unit test.xlsx'
try:
    # Đọc sheet 'Module QLT'
    df = pd.read_excel(file_path, sheet_name='Module QLT')
    
    # Hiển thị các cột để kiểm tra tên cột chính xác
    print("Columns:", df.columns.tolist())
    
    # Lọc các dòng có kết quả 'Fail' (giả định cột Kết quả có tên chứa từ 'Kết quả' hoặc 'Status')
    # Nếu không tìm thấy cột cụ thể, tôi sẽ in ra 20 dòng đầu tiên để phân tích
    print("\n--- First 20 rows of Module QLT ---\n")
    print(df.head(20).to_string())
    
    # Nếu có cột Kết quả thực tế, in các kịch bản Fail
    fail_df = df[df.iloc[:, 6].astype(str).str.contains('Fail', case=False, na=False)] 
    if not fail_df.empty:
        print("\n--- Failed Test Cases ---\n")
        print(fail_df.to_string())
    else:
        print("\nNo explicitly marked 'Fail' found in column 6, please check the output above.")

except Exception as e:
    print(f"Error reading Excel: {e}")
