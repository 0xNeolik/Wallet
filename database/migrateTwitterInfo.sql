-- Add missing fields to users table
alter table users add column screenname VARCHAR
alter table users add column avatar_url VARCHAR

-- Copies the required attributes from twitter_accounts into users
update users set screenname=tw.screenname, avatar_url=tw.avatar_url from twitter_accounts as tw where tw.user_id=users.id;

