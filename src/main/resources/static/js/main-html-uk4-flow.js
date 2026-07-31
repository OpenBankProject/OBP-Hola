// renderObpError comes from main-html-obp-error.js -- OBP-API refuses a call whose consent does
// not cover it (403 OBP-20017), is not AUTHORISED (401 OBP-35005), belongs to another standard
// (OBP-35036) or another consumer (OBP-35015), and each of those must be visible rather than
// rendering as an empty result box.
$(function () {
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
});
