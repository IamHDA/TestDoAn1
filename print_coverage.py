import csv
import os
from collections import defaultdict

jacoco_csv = r"target/site/jacoco/jacoco.csv"

output_class_csv = r"target/site/jacoco/coverage_by_class.csv"
output_module_csv = r"target/site/jacoco/coverage_by_module.csv"

if not os.path.exists(jacoco_csv):
    print("Khong tim thay jacoco.csv. Hay chay mvn clean test truoc.")
    exit(1)


def pct(covered, missed):
    total = covered + missed
    if total == 0:
        return 100.0
    return covered / total * 100.0


def to_float(value):
    return round(value, 2)


def get_module_name(class_name):
    """
    AnnouncementServiceImpl -> Announcement
    CommentServiceImpl -> Comment
    ClassroomStatisticsServiceImpl -> ClassroomStatistics
    UserServiceImpl -> User
    """
    if class_name.endswith("ServiceImpl"):
        return class_name[:-len("ServiceImpl")]
    if class_name.endswith("Impl"):
        return class_name[:-len("Impl")]
    return class_name


def get_test_file_name(class_name):
    """
    AnnouncementServiceImpl -> AnnouncementServiceImplTest.java
    """
    return f"{class_name}Test.java"


class_rows = []
module_totals = defaultdict(lambda: {
    "instruction_covered": 0,
    "instruction_missed": 0,
    "branch_covered": 0,
    "branch_missed": 0,
    "method_covered": 0,
    "method_missed": 0,
    "line_covered": 0,
    "line_missed": 0,
    "classes": []
})

with open(jacoco_csv, "r", encoding="utf-8") as f:
    reader = csv.DictReader(f)

    for row in reader:
        package_name = row["PACKAGE"]
        class_name = row["CLASS"]

        # Chỉ lấy các service impl bạn đang test.
        # Nếu muốn lấy tất cả class thì comment block if này lại.
        if "services.impl" not in package_name:
            continue

        if not class_name.endswith("ServiceImpl"):
            continue

        instruction_missed = int(row["INSTRUCTION_MISSED"])
        instruction_covered = int(row["INSTRUCTION_COVERED"])
        branch_missed = int(row["BRANCH_MISSED"])
        branch_covered = int(row["BRANCH_COVERED"])
        line_missed = int(row["LINE_MISSED"])
        line_covered = int(row["LINE_COVERED"])
        complexity_missed = int(row["COMPLEXITY_MISSED"])
        complexity_covered = int(row["COMPLEXITY_COVERED"])
        method_missed = int(row["METHOD_MISSED"])
        method_covered = int(row["METHOD_COVERED"])

        module_name = get_module_name(class_name)
        impl_file = f"{class_name}.java"
        test_file = get_test_file_name(class_name)

        class_result = {
            "module": module_name,
            "package": package_name,
            "impl_file": impl_file,
            "test_file": test_file,

            "instruction_covered": instruction_covered,
            "instruction_missed": instruction_missed,
            "instruction_coverage_percent": to_float(pct(instruction_covered, instruction_missed)),

            "branch_covered": branch_covered,
            "branch_missed": branch_missed,
            "branch_coverage_percent": to_float(pct(branch_covered, branch_missed)),

            "method_covered": method_covered,
            "method_missed": method_missed,
            "method_coverage_percent": to_float(pct(method_covered, method_missed)),

            "line_covered": line_covered,
            "line_missed": line_missed,
            "line_coverage_percent": to_float(pct(line_covered, line_missed)),

            "complexity_covered": complexity_covered,
            "complexity_missed": complexity_missed,
            "complexity_coverage_percent": to_float(pct(complexity_covered, complexity_missed)),
        }

        class_rows.append(class_result)

        totals = module_totals[module_name]
        totals["instruction_covered"] += instruction_covered
        totals["instruction_missed"] += instruction_missed
        totals["branch_covered"] += branch_covered
        totals["branch_missed"] += branch_missed
        totals["method_covered"] += method_covered
        totals["method_missed"] += method_missed
        totals["line_covered"] += line_covered
        totals["line_missed"] += line_missed
        totals["classes"].append(class_result)


class_rows.sort(key=lambda x: x["module"])

os.makedirs(os.path.dirname(output_class_csv), exist_ok=True)

class_headers = [
    "module",
    "package",
    "impl_file",
    "test_file",

    "instruction_covered",
    "instruction_missed",
    "instruction_coverage_percent",

    "branch_covered",
    "branch_missed",
    "branch_coverage_percent",

    "method_covered",
    "method_missed",
    "method_coverage_percent",

    "line_covered",
    "line_missed",
    "line_coverage_percent",

    "complexity_covered",
    "complexity_missed",
    "complexity_coverage_percent",
]

with open(output_class_csv, "w", encoding="utf-8-sig", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=class_headers)
    writer.writeheader()
    writer.writerows(class_rows)


module_rows = []

total_instruction_covered = 0
total_instruction_missed = 0
total_branch_covered = 0
total_branch_missed = 0
total_method_covered = 0
total_method_missed = 0
total_line_covered = 0
total_line_missed = 0

for module_name in sorted(module_totals.keys()):
    totals = module_totals[module_name]

    instruction_covered = totals["instruction_covered"]
    instruction_missed = totals["instruction_missed"]
    branch_covered = totals["branch_covered"]
    branch_missed = totals["branch_missed"]
    method_covered = totals["method_covered"]
    method_missed = totals["method_missed"]
    line_covered = totals["line_covered"]
    line_missed = totals["line_missed"]

    total_instruction_covered += instruction_covered
    total_instruction_missed += instruction_missed
    total_branch_covered += branch_covered
    total_branch_missed += branch_missed
    total_method_covered += method_covered
    total_method_missed += method_missed
    total_line_covered += line_covered
    total_line_missed += line_missed

    impl_files = "; ".join(sorted(item["impl_file"] for item in totals["classes"]))
    test_files = "; ".join(sorted(item["test_file"] for item in totals["classes"]))

    module_rows.append({
        "module": module_name,
        "impl_files": impl_files,
        "test_files": test_files,

        "instruction_covered": instruction_covered,
        "instruction_missed": instruction_missed,
        "instruction_coverage_percent": to_float(pct(instruction_covered, instruction_missed)),

        "branch_covered": branch_covered,
        "branch_missed": branch_missed,
        "branch_coverage_percent": to_float(pct(branch_covered, branch_missed)),

        "method_covered": method_covered,
        "method_missed": method_missed,
        "method_coverage_percent": to_float(pct(method_covered, method_missed)),

        "line_covered": line_covered,
        "line_missed": line_missed,
        "line_coverage_percent": to_float(pct(line_covered, line_missed)),
    })


summary_row = {
    "module": "ALL_SERVICE_IMPL",
    "impl_files": "",
    "test_files": "",

    "instruction_covered": total_instruction_covered,
    "instruction_missed": total_instruction_missed,
    "instruction_coverage_percent": to_float(pct(total_instruction_covered, total_instruction_missed)),

    "branch_covered": total_branch_covered,
    "branch_missed": total_branch_missed,
    "branch_coverage_percent": to_float(pct(total_branch_covered, total_branch_missed)),

    "method_covered": total_method_covered,
    "method_missed": total_method_missed,
    "method_coverage_percent": to_float(pct(total_method_covered, total_method_missed)),

    "line_covered": total_line_covered,
    "line_missed": total_line_missed,
    "line_coverage_percent": to_float(pct(total_line_covered, total_line_missed)),
}

module_headers = [
    "module",
    "impl_files",
    "test_files",

    "instruction_covered",
    "instruction_missed",
    "instruction_coverage_percent",

    "branch_covered",
    "branch_missed",
    "branch_coverage_percent",

    "method_covered",
    "method_missed",
    "method_coverage_percent",

    "line_covered",
    "line_missed",
    "line_coverage_percent",
]

with open(output_module_csv, "w", encoding="utf-8-sig", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=module_headers)
    writer.writeheader()
    writer.writerow(summary_row)
    writer.writerows(module_rows)

print(f"Exported: {output_class_csv}")
print(f"Exported: {output_module_csv}")