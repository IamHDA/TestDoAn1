import pandas as pd
import re
import os

base_dir = r'd:\This Semester\Quality Assurance\TestDoAn1\src\test\java\com\vn\backend\unit'

def process_csv(csv_path):
    # read without skipping, find the header row
    df = pd.read_csv(csv_path, header=None)
    header_idx = -1
    for i in range(len(df)):
        if str(df.iloc[i, 0]).strip() == 'TestcaseID':
            header_idx = i
            break
            
    if header_idx == -1:
        print(f"Header not found in {csv_path}")
        return
        
    df = pd.read_csv(csv_path, skiprows=header_idx)
    for index, row in df.iterrows():
        tc_id = str(row['TestcaseID']).strip()
        if 'Lớp' not in df.columns or 'Phương thức' not in df.columns or 'Mục tiêu kiểm thử' not in df.columns:
             print("Missing columns")
             continue
        class_name = str(row['Lớp']).strip()
        method_name = str(row['Phương thức']).strip()
        objective = str(row['Mục tiêu kiểm thử']).strip()
        
        if pd.isna(tc_id) or pd.isna(class_name) or pd.isna(method_name) or tc_id == 'nan' or class_name == 'nan':
            continue
            
        file_path = os.path.join(base_dir, class_name + '.java')
        if not os.path.exists(file_path):
            print(f'File not found: {file_path}')
            continue
            
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            
        import_stmt = 'import org.junit.jupiter.api.DisplayName;\n'
        if 'import org.junit.jupiter.api.DisplayName;' not in content:
            content = re.sub(r'(package .*;\n)', r'\1\n\n' + import_stmt, content, count=1)
            
        escaped_objective = objective.replace('\"', '\\\"').replace('\n', ' ')
        display_name_annotation = f'@DisplayName(\"{tc_id} - {escaped_objective}\")'
        
        if display_name_annotation in content: # avoid exact duplicate
            continue
            
        pattern = r'(\s+)(@Test)(\s+void\s+' + re.escape(method_name) + r'\s*\()'
        
        def replacer(match):
            indent = match.group(1)
            return f'{indent}{match.group(2)}{indent}{display_name_annotation}{match.group(3)}'
            
        new_content, count = re.subn(pattern, replacer, content)
        if count == 0:
            pattern2 = r'(\s+)(void\s+' + re.escape(method_name) + r'\s*\()'
            def replacer2(match):
                indent = match.group(1)
                return f'{indent}{display_name_annotation}{indent}{match.group(2)}'
            
            new_content, count2 = re.subn(pattern2, replacer2, content)
            if count2 > 0:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f'Updated {method_name} in {class_name}')
            else:
                print(f'Method not found: {method_name} in {class_name}')
        else:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f'Updated {method_name} in {class_name}')

process_csv('Module_QLHT.csv')
process_csv('Module_QLLH.csv')
