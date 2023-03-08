"""This module consists of common utilities to work with persistence."""

from __future__ import annotations

import json
import logging
import os
import typing as _t
from collections import defaultdict
from importlib import import_module

if _t.TYPE_CHECKING:  # pragma: no cover
    # imported objects for function type hint, completion, etc.
    # these won't be executed in runtime
    from jans.pycloudlib.manager import Manager

logger = logging.getLogger(__name__)


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
        self._clients: dict[str, _t.Any] = {}
        self._manager = manager

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
                self._mapping, _ = self.resolve_hybrid_mapping()
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
    def resolve_hybrid_mapping(cls) -> tuple[dict[str, str], str]:
        """Validate the value of ``hybrid_mapping`` attribute."""
        mapping = json.loads(os.environ.get("CN_HYBRID_MAPPING", "{}"))

        # only allow `dict` format
        if not isinstance(mapping, dict):
            return {}, "Invalid hybrid mapping format"

        # build whitelisted mapping based on the following rules:
        #
        # - each key must be one of PERSISTENCE_DATA_KEYS
        # - each value must be one of PERSISTENCE_TYPES (prefix also considered as valid)
        #
        # note that prefix means anything that comes before first `.` character
        sanitized_mapping = {}

        for k, v in mapping.items():
            if k not in PERSISTENCE_DATA_KEYS:
                continue

            prefix = v.split(".", 1)[0]
            if prefix not in PERSISTENCE_TYPES:
                continue

            # item is valid
            sanitized_mapping[k] = v

        # keys must be as similar as PERSISTENCE_DATA_KEYS
        if sorted(sanitized_mapping.keys()) != sorted(PERSISTENCE_DATA_KEYS):
            return {}, f"Either key(s) or value(s) is missing or invalid in hybrid mapping {mapping}"

        # mapping is valid (without error)
        return sanitized_mapping, ""

    @property
    def manager(self) -> Manager:
        """Get jans.pycloudlib.manager.Manager instance."""
        if not self._manager:
            from jans.pycloudlib import get_manager

            logger.warning(
                f"Manager object is not set when instantiating the {self.__class__.__name__} object, "
                "thus it will be automatically created. Note that this behaviour will be "
                "deprecated and changed in the future.",
            )
            self._manager = get_manager()
        return self._manager

    @property
    def clients(self) -> dict[str, _t.Any]:
        if not self._clients:
            self._clients = {g: self._resolve_client(g) for g in self.groups()}
        return self._clients

    def get_mapping_client(self, mapping: str) -> _t.Any:
        name: str = self.mapping.get(mapping, "")
        return self.clients.get(name)

    def _resolve_client(self, group: str) -> _t.Any:
        client_mappings = {
            "sql": ("jans.pycloudlib.persistence.sql", "SqlClient"),
            "couchbase": ("jans.pycloudlib.persistence.couchbase", "CouchbaseClient"),
            "spanner": ("jans.pycloudlib.persistence.spanner", "SpannerClient"),
            "ldap": ("jans.pycloudlib.persistence.ldap", "LdapClient"),
        }

        client = None
        for name in client_mappings:
            if group.startswith(name):
                mod, clsname = client_mappings.get(name) or [None, None]

                if not (mod and clsname):
                    continue

                client_cls = getattr(import_module(mod), clsname)
                kwargs = {
                    "env_suffix": group.removeprefix(name).upper().replace(".", "_EXT_")
                }
                client = client_cls(self.manager, **kwargs)
                break
        return client  # noqa: R504
