$(function () {
    // OBP-API rejects a UK v4.0.1 data call whose token is not bound to a usable consent. Those
    // rejections carry the reason in the OBP error code, so render it rather than swallowing it --
    // without this a 403 from checkUKConsent shows up as nothing at all.
    //   OBP-35035 the access token has no consent_id claim
    //   OBP-35036 the consent belongs to a different API standard
    //   OBP-35023 the consent is bound to a different user
    //   OBP-35005 the consent is not (or no longer) AUTHORISED
    function renderObpError(target, xhr) {
        let detail = xhr.responseText || 'no response body';
        try {
            const parsed = JSON.parse(xhr.responseText);
            if (parsed && parsed.message) {
                detail = parsed.message;
            }
        } catch (e) {
            // Not JSON — fall back to the raw body.
        }
        target.append(
            `<div class="alert alert-danger" data-testid="uk4-error">HTTP ${xhr.status}: ${detail}</div>`);
    }

    $('#get_accounts_uk4').click(function () {
        const container = $('#account_list_uk4').empty().append('<h1>Account List (UK Open Banking v4.0.1):</h1>');
        $.getJSON("/account_uk4", function (data) {
            $.each(data.Account, function (index, account) {
                let zson = JSON.stringify(account, null, 2);
                container.append(`
                <div>
                    <code>${zson}</code><br>
                    <button class="get_account_detail_uk4" account_id="${account['AccountId']}">Get Account detail</button>
                    <button class="get_balances_uk4" account_id="${account['AccountId']}">Get Balances</button>
                    <button class="get_transactions_uk4" account_id="${account['AccountId']}">Get Transactions</button>
                    <div class="account_detail_uk4"></div>
                    <div class="balances_uk4"></div>
                    <div class="transactions_uk4"></div>
                    <hr>
                </div>
            `);
            });
            $('.get_account_detail_uk4').click(function () {
                let accountDetailEle = $(this).siblings('.account_detail_uk4').empty().append('<h3>Account Detail:</h3>');
                let accountId = $(this).attr('account_id');
                $.getJSON('/account_uk4/' + accountId, function (data) {
                    let zson = JSON.stringify(data, null, 2);
                    accountDetailEle.append(`<code>${zson}<code>`).append('<br>');
                }).fail(function (xhr) {
                    renderObpError(accountDetailEle, xhr);
                });
            });
            $('.get_balances_uk4').click(function () {
                let balancesEle = $(this).siblings('.balances_uk4').empty().append('<h3>Balance List:</h3>');
                let accountId = $(this).attr('account_id');
                $.getJSON('/balances_uk4/account_id/' + accountId, function (data) {
                    let zson = JSON.stringify(data, null, 2);
                    balancesEle.append(`<code>${zson}<code>`).append('<br>');
                }).fail(function (xhr) {
                    renderObpError(balancesEle, xhr);
                });
            });
            $('.get_transactions_uk4').click(function () {
                let accountsEle = $(this).siblings('.transactions_uk4').empty().append('<h3>Transaction List:</h3>');
                let accountId = $(this).attr('account_id');
                $.getJSON('/transactions_uk4/account_id/' + accountId, function (data) {
                    let zson = JSON.stringify(data, null, 2);
                    accountsEle.append(`<code>${zson}<code>`).append('<br>');
                }).fail(function (xhr) {
                    renderObpError(accountsEle, xhr);
                });
            });
        }).fail(function (xhr) {
            renderObpError(container, xhr);
        });
    });

    // Negative control: same endpoint, but the server side authenticates with a client-credentials
    // token that carries no consent_id claim. A 403 OBP-35035 here is the expected — and desired —
    // outcome: it demonstrates the data is gated on the consent, not merely on being authenticated.
    $('#get_accounts_uk4_no_consent').click(function () {
        const container = $('#account_list_uk4_no_consent').empty()
            .append('<h3>Same endpoint, token without a consent_id claim:</h3>');
        $.getJSON("/account_uk4_no_consent", function (data) {
            container.append(
                `<div class="alert alert-warning" data-testid="uk4-no-consent-unexpected">Unexpected success — the consent gate did not reject a token with no consent: <code>${JSON.stringify(data, null, 2)}</code></div>`);
        }).fail(function (xhr) {
            container.append(
                '<div data-testid="uk4-no-consent-expected">Rejected as expected — access is consent-gated:</div>');
            renderObpError(container, xhr);
        });
    });
});
