import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('goals', Path(__file__).resolve().parents[1] / 'generate_goals.py')
goals = importlib.util.module_from_spec(spec)
spec.loader.exec_module(goals)


class EditableGoalsTest(unittest.TestCase):
    def setUp(self):
        cache = Path(__file__).resolve().parents[2] / '.reference-cache'
        cache.mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(prefix='goals-test-', dir=cache)
        if Path(self.temp.name).resolve().parent != cache.resolve():
            raise ValueError('Test directory outside project cache')
        self.original = goals.RES
        goals.RES = Path(self.temp.name)
        rows = [
            {'id': 1, 'name': 'Old armour', 'released': '2005-06-22'},
            {'id': 2, 'name': 'Old armour', 'released': '2005-06-22', 'noted': True},
            {'id': 3, 'name': 'New armour', 'released': '2005-06-23'},
            {'id': 4, 'name': 'Old armour', 'released': '2019-01-01', 'replacementOf': 1},
            {'id': 5, 'name': 'Unknown armour', 'released': None},
        ]
        (goals.RES / 'historical-items.json').write_text(json.dumps(rows), encoding='utf-8')

    def tearDown(self):
        goals.RES = self.original
        self.temp.cleanup()

    def generate(self, definitions):
        (goals.RES / 'goal-definitions.json').write_text(json.dumps(definitions), encoding='utf-8')
        goals.generate()
        return json.loads((goals.RES / 'historical-goals.json').read_text(encoding='utf-8'))

    def definition(self, **changes):
        result = {'key': 'my-choice', 'name': 'My armour', 'group': 'My BIS', 'items': ['Old armour']}
        result.update(changes)
        return result

    def test_names_allow_replacement_but_exclude_notes(self):
        self.assertEqual([1, 4], self.generate([self.definition()])[0]['alternatives'])

    def test_exact_id_can_limit_alternatives(self):
        self.assertEqual([1], self.generate([self.definition(items=[], itemIds=[1])])[0]['alternatives'])

    def test_modern_and_unknown_items_are_rejected(self):
        for value in (3, 5, 999):
            with self.subTest(value=value), self.assertRaises(ValueError):
                self.generate([self.definition(items=[], itemIds=[value])])

    def test_duplicate_keys_are_rejected(self):
        with self.assertRaises(ValueError):
            self.generate([self.definition(), self.definition()])

    def test_no_fixed_bis_groups_are_required(self):
        result = self.generate([self.definition(key='custom', group='Kirk choices')])
        self.assertEqual('Kirk choices', result[0]['group'])

    def test_invalid_edit_does_not_replace_previous_checklist(self):
        expected = self.generate([self.definition()])
        with self.assertRaises(ValueError):
            self.generate([self.definition(items=['Misspelled armour'])])
        self.assertEqual(expected, json.loads((goals.RES / 'historical-goals.json').read_text()))
