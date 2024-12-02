#include "ledger_wrap/global.h"

#include <mutex>

// First <system.hh> before any other Ledger headers
#include <system.hh>
#include <scope.h>
#include <session.h>

extern int main(int argc, char* argv[], char* envp[]);

namespace ledger_wrap
{

void init_ledger_globals(ledger::session_t *session)
{
    static std::mutex initialized_lock;
    static bool initialized = false;

    std::lock_guard<std::mutex> lock(initialized_lock);

    // If we have already initialized, do nothing -- Ledger is ready.
    if (initialized) {
        return;
    }

    // Initialize the global empty scope singleton, and make it the default scope.
    static ledger::empty_scope_t empty_scope;
    ledger::scope_t::empty_scope   = &empty_scope;
    ledger::scope_t::default_scope = &empty_scope;

    // This call is not thread-safe and requires a mutex. It looks like it depends
    // on the session, but in fact it does not (at least in 3.2.1) and initializes
    // some other important global stuff. However, the pointer *must* be non-null.
    ledger::set_session_context(session);

    initialized = true;
}

int call_main(int argc, char * argv[], char * envp[])
{
    return main(argc, argv, envp);
}

} // namespace ledger_wrap
