-- film.sql

-- name: fetch
-- fn: first
select *
from film
where id = :id

-- name: all
select film_id, title, description, release_year, rating, length
from film
limit 50
-- order by created_at desc

-- name: insert
-- fn: first
insert into film (id, name, created_at)
values (:id, :name, :created_at)
returning *

-- name: update
-- fn: first
update film
set name = :name
where id = :id
returning *

-- name: delete
-- fn: first
delete
from film
where id = :id
returning *