// renderObpError comes from main-html-obp-error.js -- OBP-API refuses a call whose consent does
// not cover it (403 OBP-20017), is not AUTHORISED (401 OBP-35005), belongs to another standard
// (OBP-35036) or another consumer (OBP-35015), and each of those must be visible rather than
// rendering as an empty result box.
$(function () {
    // The account list is the one call that can be refused without an error. OBP-API filters out
    // accounts the consent grants no view on, so a consent without ReadAccountsBasic or
    // ReadAccountsDetail gets 200 with an empty Account array -- there is no failure for .fail()
    // to catch, and the page would otherwise render a heading and nothing else.
    //
    // UK v4.0.1 requires a consent to carry at least one of those two permissions (see the
    // Account and Transaction API profile), precisely because every other endpoint is
    // /accounts/{AccountId}/... : without them the account ids are undiscoverable and the consent
    // is a dead end, even though balances and transactions would answer for an id known some
    // other way.
    function grantedPermissions() {
        const raw = $('[data-testid="uk4-consent-panel"]').attr('data-uk4-permissions') || '';
        return raw.split(',').map(p => p.trim()).filter(p => p.length > 0);
    }

    function explainEmptyAccountList(target) {
        const permissions = grantedPermissions();
        const canReadAccounts = permissions.includes('ReadAccountsBasic')
            || permissions.includes('ReadAccountsDetail');

        if (!canReadAccounts) {
            target.append(
                '<div class="alert alert-warning" data-testid="uk4-accounts-empty-no-permission">' +
                '<b>No accounts returned — the consent grants neither ReadAccountsBasic nor ' +
                'ReadAccountsDetail.</b><br>' +
                'The call itself succeeded (HTTP 200): OBP-API filters out every account the consent ' +
                'grants no view on, so the list comes back empty rather than as an error.<br>' +
                'Granted: <code>' + (permissions.join(', ') || '(none)') + '</code><br>' +
                'Create a new consent including ReadAccountsBasic or ReadAccountsDetail — UK v4.0.1 ' +
                'requires one of them.</div>');
            return;
        }

        target.append(
            '<div class="alert alert-warning" data-testid="uk4-accounts-empty">' +
            '<b>No accounts returned.</b><br>' +
            'The consent does grant an account-read permission, so this is not a permission problem: ' +
            'the consent is most likely bound to accounts other than the ones you hold at this bank.' +
            '</div>');
    }

    $('#get_accounts_uk4').click(function () {
        const container = $('#account_list_uk4').empty().append('<h1>Account List (UK Open Banking v4.0.1):</h1>');
        $.getJSON("/account_uk4", function (data) {
            const accounts = (data && data.Account) || [];
            if (accounts.length === 0) {
                explainEmptyAccountList(container);
                return;
            }
            $.each(accounts, function (index, account) {
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
});
