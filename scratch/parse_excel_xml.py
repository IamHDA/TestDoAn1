import xml.etree.ElementTree as ET

base_path = r'd:\CC\SQA\Code BE-TEST\TestDoAn1\scratch\excel_unzipped\xl'
shared_strings_file = base_path + r'\sharedStrings.xml'
sheet_file = base_path + r'\worksheets\sheet6.xml'

try:
    # 1. Parse sharedStrings.xml
    tree = ET.parse(shared_strings_file)
    root = tree.getroot()
    
    # Excel uses a namespace
    ns = {'ns': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
    
    strings = []
    for si in root.findall('ns:si', ns):
        t = si.find('ns:t', ns)
        if t is not None:
            strings.append(t.text)
        else:
            # Handle rich text cases if necessary
            full_text = "".join([node.text for node in si.findall('.//ns:t', ns) if node.text])
            strings.append(full_text)

    # Find index of "Fail"
    fail_indices = [i for i, s in enumerate(strings) if s and "Fail" in s]
    print(f"Indices for 'Fail': {fail_indices}")
    
    if not fail_indices:
        print("No 'Fail' found in sharedStrings.xml")
        # Maybe it's literal? Some tools don't use shared strings for everything.
        # But usually Excel does.

    # 2. Parse sheet6.xml
    sheet_tree = ET.parse(sheet_file)
    sheet_root = sheet_tree.getroot()
    
    rows_data = []
    for row in sheet_root.findall('.//ns:row', ns):
        row_cells = []
        is_fail_row = False
        for cell in row.findall('ns:c', ns):
            cell_type = cell.get('t')
            v_node = cell.find('ns:v', ns)
            if v_node is not None:
                val = v_node.text
                if cell_type == 's':
                    str_val = strings[int(val)] if int(val) < len(strings) else "UNKNOWN"
                    row_cells.append(str_val)
                    if int(val) in fail_indices:
                        is_fail_row = True
                else:
                    row_cells.append(val)
            else:
                row_cells.append("")
        
        # If it's a fail row or header row (row index 1 or 2 usually)
        if is_fail_row or int(row.get('r')) < 5:
            rows_data.append((row.get('r'), row_cells))

    print("\n--- Rows related to Failures ---\n")
    for r_idx, data in rows_data:
        print(f"Row {r_idx}: {data}")

except Exception as e:
    print(f"Error: {e}")
