$(function () {
    $('#get_accounts_uk4').click(function () {
        $.getJSON("/account_uk4", function (data) {
            const container = $('#account_list_uk4').empty().append('<h1>Account List (UK Open Banking v4.0.1):</h1>');
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
                });
            });
            $('.get_balances_uk4').click(function () {
                let balancesEle = $(this).siblings('.balances_uk4').empty().append('<h3>Balance List:</h3>');
                let accountId = $(this).attr('account_id');
                $.getJSON('/balances_uk4/account_id/' + accountId, function (data) {
                    let zson = JSON.stringify(data, null, 2);
                    balancesEle.append(`<code>${zson}<code>`).append('<br>');
                });
            });
            $('.get_transactions_uk4').click(function () {
                let accountsEle = $(this).siblings('.transactions_uk4').empty().append('<h3>Transaction List:</h3>');
                let accountId = $(this).attr('account_id');
                $.getJSON('/transactions_uk4/account_id/' + accountId, function (data) {
                    let zson = JSON.stringify(data, null, 2);
                    accountsEle.append(`<code>${zson}<code>`).append('<br>');
                });
            });
        });
    });
});
