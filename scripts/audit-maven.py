#!/usr/bin/env python3
"""Thin OSV API client for Gradle's resolved production dependencies. No credentials."""
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request


def main():
    directory = Path(__file__).resolve().parent.parent / 'backend/build/reports/dependencies'
    queries = json.loads((directory / 'runtime.json').read_text())
    pending = list(enumerate(queries))
    findings = [[] for _ in queries]
    while pending:
        request = urllib.request.Request(
            'https://api.osv.dev/v1/querybatch',
            json.dumps({'queries': [query for _, query in pending]}).encode(),
            {'Content-Type': 'application/json'},
        )
        with urllib.request.urlopen(request, timeout=45) as response:
            results = json.load(response)['results']
        if len(results) != len(pending):
            raise ValueError('Incomplete OSV response')
        next_page = []
        for (index, query), result in zip(pending, results):
            findings[index].extend(result.get('vulns', []))
            if result.get('next_page_token'):
                next_page.append((index, {**query, 'page_token': result['next_page_token']}))
        pending = next_page
    report = [{**q, 'advisories': v} for q, v in zip(queries, findings) if v]
    (directory / 'osv.json').write_text(json.dumps(report, indent=2) + '\n')
    for row in report:
        print(row['package']['name'], row['version'], ', '.join(v['id'] for v in row['advisories']))
    print(f'OSV: {len(queries)} runtime packages checked, {len(report)} with advisories requiring review.')
    return 1 if report else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, urllib.error.URLError) as error:
        print(f'OSV scan incomplete: {error}', file=sys.stderr)
        sys.exit(2)
