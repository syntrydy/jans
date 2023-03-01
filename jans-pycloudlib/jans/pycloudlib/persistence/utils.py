"""This module consists of common utilities to work with persistence."""

from __future__ import annotations

import json
import os
import typing as _t
from collections import defaultdict

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager


def render_salt(manager: Manager, src: str, dest: str) -> None:
    """Render file contains salt string.

    The generated file has the following contents:

    ```py
    encode_salt = random-salt-string
    ```

    Args:
        manager: An instance of manager class.
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    encode_salt = manager.secret.get("encoded_salt")

    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {"encode_salt": encode_salt}
        f.write(rendered_txt)


def render_base_properties(src: str, dest: str) -> None:
    """Render file contains properties for Janssen Server.

    Args:
        src: Absolute path to the template.
        dest: Absolute path where generated file is located.
    """
    with open(src) as f:
        txt = f.read()

    with open(dest, "w") as f:
        rendered_txt = txt % {
            "persistence_type": os.environ.get("CN_PERSISTENCE_TYPE", "ldap"),
        }
        f.write(rendered_txt)


#: Supported persistence types.
PERSISTENCE_TYPES = (
    "ldap",
    "couchbase",
    "sql",
    "spanner",
    "hybrid",
)
"""Supported persistence types."""

PERSISTENCE_DATA_KEYS = (
    "default",
    "user",
    "site",
    "cache",
    "token",
    "session",
    "configuration",
)
"""Data mapping of persistence, ordered by priority."""

PERSISTENCE_SQL_DIALECTS = (
    "mysql",
    "pgsql",
)
"""SQL dialects."""

RDN_MAPPING = {
    "default": "",
    "user": "people, groups, authorizations",
    "cache": "cache",
    "site": "cache-refresh",
    "token": "tokens",
    "session": "sessions",
    "configuration": "configuration",
}
"""Mapping of RDN (Relative Distinguished Name)."""


class PersistenceMapper:
    """This class creates persistence data mapping.

    Example of data mapping when using ``sql`` persistence type:

    ```py
    os.environ["CN_PERSISTENCE_TYPE"] = "sql"
    mapper = PersistenceMapper()
    print(mapper.mapping)
    ```

    The output will be:

    ```py
    {
        "default": "sql",
        "user": "sql",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
        "configuration": "sql",
    }
    ```

    The same rule applies to any supported persistence types, except for ``hybrid``
    where each key can have different value. To customize the mapping, additional environment
    variable is required.

    ```py
    os.environ["CN_PERSISTENCE_TYPE"] = "hybrid"
    os.environ["CN_HYBRID_MAPPING"] = json.loads({
        "default": "sql",
        "user": "spanner",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "couchbase",
        "configuration": "sql",
    })

    mapper = PersistenceMapper()
    print(mapper.mapping)
    ```

    The output will be:

    ```py
    {
        "default": "sql",
        "user": "spanner",
        "site": "sql",
        "cache": "sql",
        "token": "sql",
        "session": "sql",
        "configuration": "sql",
    }
    ```

    Note that when using ``hybrid``, all mapping must be defined explicitly.
    """

    def __init__(self, manager: _t.Optional[Manager] = None) -> None:
        self._mapping: dict[str, str] = {}

    @property
    def mapping(self) -> dict[str, str]:
        """Pre-populate a key-value pair of persistence data (if empty).

        Example of pre-populated mapping:

        ```py
        {
            "default": "sql",
            "user": "spanner",
            "site": "sql",
            "cache": "sql",
            "token": "sql",
            "session": "sql",
            "configuration": "sql",
        }
        ```
        """
        if not self._mapping:
            type_ = os.environ.get("CN_PERSISTENCE_TYPE", "ldap")

            if type_ != "hybrid":
                self._mapping = dict.fromkeys(PERSISTENCE_DATA_KEYS, type_)
            else:
                mapping = json.loads(os.environ.get("CN_HYBRID_MAPPING", "{}"))
                self._mapping = dict.fromkeys(PERSISTENCE_DATA_KEYS, "ldap")
                self._mapping.update({
                    k: v for k, v in mapping.items()
                    if k in PERSISTENCE_DATA_KEYS
                })
        return self._mapping

    def groups(self) -> dict[str, list[str]]:
        """Pre-populate mapping groupped by persistence type.

        Example of pre-populated groupped mapping:

        ```py
        {
            "sql": ["cache", "default", "session"],
            "couchbase": ["user"],
            "spanner": ["token", "configuration"],
            "ldap": ["site"],
        }
        ```
        """
        mapper = defaultdict(list)

        for k, v in self.mapping.items():
            mapper[v].append(k)
        return dict(sorted(mapper.items()))

    def groups_with_rdn(self) -> dict[str, list[str]]:
        """Pre-populate mapping groupped by persistence type and its values replaced by RDN.

        Example of pre-populated groupped mapping:

        ```py
        {
            "sql": ["cache", "sessions"],
            "couchbase": ["people, groups, authorizations"],
            "spanner": ["tokens", "configuration"],
            "ldap": ["cache-refresh"],
        }
        ```
        """
        mapper = defaultdict(list)
        for k, v in self.mapping.items():
            rdn = RDN_MAPPING[k]
            if not rdn:
                continue
            mapper[v].append(rdn)
        return dict(sorted(mapper.items()))

    @classmethod
    def validate_hybrid_mapping(cls) -> dict[str, str]:
        """Validate the value of ``hybrid_mapping`` attribute.

        This method is deprecated.
        """
        import warnings

        warnings.warn(
            "'validate_hybrid_mapping' function is now a no-op function "
            "and no longer required by hybrid persistence ",
            DeprecationWarning,
            stacklevel=2,
        )
