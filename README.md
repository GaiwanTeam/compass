# Conference Compass

Conference activities and planning app.

- Clojure backend + HTMX
- Datomic
- Discord for Auth
- Integrant
- [Ornament](https://github.com/lambdaisland/ornament) and [Open Props](https://open-props.style/)

This will be used at [Heart of Clojure](https://heartofclojure.eu), replacing
the aging [Eurucamp Activities App](https://github.com/heartofclojure/activities) which we used five years ago
(and which stopped being developed several years before that).

In the Compass app people can find the conference schedule, but also any
additional activities, and they can add their own activities, unconference
style.

People can star/bookmark or sign up for their favorite activities/sessions.
Activity organizers can limit the capacity, so e.g. if you want to take up to 5
people to the climbing gym or go for Thai dinner with up to 8 people you can do
that.

## Discord

We currently only support Discord for authentication. We understand this will
not please everyone, and in the future this may change, but for Heart of Clojure
2024 this will be the only auth option.

The reason is that we also use Discord as the conference chat (backchannel), so
people should have an account there anyway. And this allows us to do some nice
things, like create a private channel with all attendees of a certain activity.

To get started you need to set up a discord bot, and then create a
`config.local.edn` in the project root like this:

```clj
;; config.local.edn
{:discord/client-secret "..."
 :discord/bot-token     "..."
 :discord/client-id     "..."
 :discord/public-key    "..."}
```

## Roadmap

See [[notes.txt]] for a basic outline of what we have planned. We will
(probably) not be able to do all of it this edition, but we should have the
basics of creating/editing activities, and signing up for them.

Currently the initial setup is then, we have Discord OAuth working, and have a
database connection, which gets pre-seeded with the talks from the schedule. The
front page renders all activities, is somewhat mobile-friendly, and does dark
and light mode. (important ;) 

## Dev setup

```
bin/launchpad dev --go
```

## License

Copyright &copy; 2024 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.

