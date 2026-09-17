"""Synthetic, offline tests: never call GitHub, use real user data, or mutate PRs."""
import unittest

from tools.pr_overlap import is_risky_path, overlap_report, related_prs, safe_text


def pr(number, base="integration", state="open"):
    return {"number": number, "base": {"ref": base}, "state": state}


class PrOverlapTests(unittest.TestCase):
    def setUp(self):
        self.current = pr(10)

    def test_same_base_and_overlapping_android_code_blocks(self):
        files = {
            10: ["app/src/main/java/TeacherApp.kt", "docs/DESIGN_SYSTEM.md"],
            9: ["app/src/main/java/TeacherApp.kt", "docs/OTHER.md"],
        }
        report = overlap_report(self.current, [self.current, pr(9)], lambda n: files[n])
        self.assertEqual([{"number": 9, "paths": ["app/src/main/java/TeacherApp.kt"], "blocking": True}], report)

    def test_stacked_and_closed_prs_are_excluded_without_fetching_files(self):
        def cannot_fetch(_):
            self.fail("No comparison should fetch files for unrelated PRs")
        report = overlap_report(self.current, [pr(11, "feature-10"), pr(8, state="closed")], cannot_fetch)
        self.assertEqual([], report)

    def test_documentation_collision_reports_but_is_not_blocking(self):
        paths = {10: ["docs/DESIGN_SYSTEM.md"], 9: ["docs/DESIGN_SYSTEM.md"]}
        self.assertEqual([{"number": 9, "paths": ["docs/DESIGN_SYSTEM.md"], "blocking": False}],
                         overlap_report(self.current, [pr(9)], lambda n: paths[n]))

    def test_workflow_and_data_paths_block(self):
        self.assertTrue(is_risky_path(".github/workflows/ci.yml"))
        self.assertTrue(is_risky_path("app/src/androidTest/Test.kt"))
        self.assertTrue(is_risky_path("tools/pr_overlap.py"))
        self.assertFalse(is_risky_path("docs/README.md"))

    def test_no_overlap_and_comparison_order_are_stable(self):
        paths = {10: ["app/A.kt"], 9: ["app/B.kt"], 7: ["app/A.kt"]}
        self.assertEqual([7, 9], [item["number"] for item in related_prs(self.current, [pr(9), pr(7)] )])
        self.assertEqual([7], [item["number"] for item in
                               overlap_report(self.current, [pr(9), pr(7)], lambda n: paths[n])])

    def test_summary_sanitizes_untrusted_filenames(self):
        self.assertEqual("_bad__file_", safe_text("`bad\n<file>"))


if __name__ == "__main__":
    unittest.main()
