import csv
import os

csv_file = r'target/site/jacoco/jacoco.csv'

if not os.path.exists(csv_file):
    print("Khong tim thay jacoco.csv, hay chay mvn test jacoco:report truoc.")
    exit()

def pct(cov, missed):
    total = cov + missed
    if total == 0: return 100.0
    return (cov / total) * 100.0

total_inst_cov = 0; total_inst_mis = 0
total_br_cov = 0; total_br_mis = 0
total_func_cov = 0; total_func_mis = 0
total_line_cov = 0; total_line_mis = 0

files = []

with open(csv_file, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        ic = int(row['INSTRUCTION_COVERED']); im = int(row['INSTRUCTION_MISSED'])
        bc = int(row['BRANCH_COVERED']); bm = int(row['BRANCH_MISSED'])
        mc = int(row['METHOD_COVERED']); mm = int(row['METHOD_MISSED'])
        lc = int(row['LINE_COVERED']); lm = int(row['LINE_MISSED'])
        
        name = row['CLASS'] + '.java'
        if len(name) > 25: name = name[:22] + '...'
        if ic + im > 0:
            files.append({
                'name': name,
                'stmts': pct(ic, im),
                'branch': pct(bc, bm),
                'funcs': pct(mc, mm),
                'lines': pct(lc, lm)
            })
            total_inst_cov += ic; total_inst_mis += im
            total_br_cov += bc; total_br_mis += bm
            total_func_cov += mc; total_func_mis += mm
            total_line_cov += lc; total_line_mis += lm

print("--------------------------------------------------------------------------------")
print("File                      | % Stmts | % Branch | % Funcs | % Lines | Uncovered")
print("--------------------------|---------|----------|---------|---------|----------")
print(f"All files                 | {pct(total_inst_cov, total_inst_mis):7.2f} | {pct(total_br_cov, total_br_mis):8.2f} | {pct(total_func_cov, total_func_mis):7.2f} | {pct(total_line_cov, total_line_mis):7.2f} | ")
print("--------------------------|---------|----------|---------|---------|----------")

for f in sorted(files, key=lambda x: x['name']):
    print(f"{f['name']:<25} | {f['stmts']:7.2f} | {f['branch']:8.2f} | {f['funcs']:7.2f} | {f['lines']:7.2f} | ")

print("--------------------------------------------------------------------------------")
print(f"Coverage summary")
print(f"Statements : {total_inst_cov}/{total_inst_cov + total_inst_mis}")
print(f"Branches   : {total_br_cov}/{total_br_cov + total_br_mis}")
print(f"Functions  : {total_func_cov}/{total_func_cov + total_func_mis}")
print(f"Lines      : {total_line_cov}/{total_line_cov + total_line_mis}")
