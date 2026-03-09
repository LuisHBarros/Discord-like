# Architecture Decisions

## Indexing Strategy

### `messages` Table

As the `messages` table is expected to be one of the fastest-growing tables in the application, we have implemented specific composite indexes to optimize the most frequent queries.

The following indexes were added to `MessageJpaEntity`:

1. **`idx_messages_room_id_created_at` (`room_id, created_at`)**
   - **Guided by Query:** Loading the message timeline for a specific room (e.g., "Fetch recent messages in Room X ordered by time").
   - **Reasoning:** Most reads will filter by `room_id` to get messages for a specific chat room, and then order by `created_at` to present them chronologically. A composite index on these two columns allows the database to quickly locate the targeted room's messages and efficiently return them in sorted order, avoiding expensive full-table scans.

2. **`idx_messages_room_id_id` (`room_id, id`)**
   - **Guided by Query:** Cursor-based pagination (keyset pagination) when scrolling through message history (e.g., "Fetch 50 messages in Room X before Message ID Y").
   - **Reasoning:** While `created_at` can be used for time-based framing, precise pagination relies on the primary key `id` as a cursor to guarantee stable sorting and prevent missing or duplicating messages (especially if multiple messages share the exact same timestamp). This index heavily optimizes queries that seek specific pages of messages for a room using the message ID as a reliable anchor.
