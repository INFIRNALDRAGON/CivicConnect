import glob
import sys
import xml.etree.ElementTree as ET


def totals(pattern):
    tests = failures = errors = skipped = 0
    for path in glob.glob(pattern):
        suite = ET.parse(path).getroot()
        tests += int(suite.get("tests", 0))
        failures += int(suite.get("failures", 0))
        errors += int(suite.get("errors", 0))
        skipped += int(suite.get("skipped", 0))
    return tests, failures, errors, skipped


def main():
    backend = sys.argv[1]
    min_unit = int(sys.argv[2])
    min_integration = int(sys.argv[3])
    problems = []
    for label, pattern, minimum in (
        ("unit", f"{backend}/target/surefire-reports/TEST-*.xml", min_unit),
        ("integration", f"{backend}/target/failsafe-reports/TEST-*.xml", min_integration),
    ):
        tests, failures, errors, skipped = totals(pattern)
        print(f"{label}: {tests} run, {failures} failed, {errors} errors, {skipped} skipped (expected at least {minimum})")
        if tests < minimum:
            problems.append(f"only {tests} {label} tests ran, expected at least {minimum}")
        if skipped:
            problems.append(f"{skipped} {label} tests were skipped")
        if failures or errors:
            problems.append(f"{failures + errors} {label} tests failed")
    if problems:
        print("TEST COUNT CHECK FAILED: " + "; ".join(problems))
        sys.exit(1)
    print("Test count check passed")


main()
