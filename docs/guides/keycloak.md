# Editing the Keycloak realm

**Last verified:** 2026-10-18 against `keycloak/realm/cinx-realm.json`

The realm is one JSON file imported by the Keycloak container. Several clients carry
identical blocks (protocol mappers, role lists), so an edit that matches text rather than a
place can change clients it was not meant for: one search-and-replace once changed seven
clients instead of two.

- Anchor every edit on text unique to the target client, such as its `clientId`, or list
  what a replace-all will touch before running it.
- After the edit, check the result structurally, not by eye:

  ```bash
  python3 -c "import json; r=json.load(open('keycloak/realm/cinx-realm.json')); [print(c['clientId'], [m['name'] for m in c.get('protocolMappers', [])]) for c in r['clients']]"
  ```
- A realm change reaches a running Keycloak only on a fresh import; say how it was applied
  when handing the change over.
