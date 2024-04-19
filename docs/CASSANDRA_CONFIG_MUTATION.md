The following configuration are causing issues while mutating in Cassandra:

- `native_transport_max_negotiable_version` (supported values 3 <= x <= 4)
- `repair_session_max_tree_depth` (supported values x >= 10)
