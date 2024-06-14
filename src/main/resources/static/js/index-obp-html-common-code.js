function setBankSelection() {
    let bank = document.querySelector("#bank");
    localStorage.setItem('last-bank-selection-obp-flow', bank.value);
}

function restoreLastBankSelection() {
    if(localStorage.getItem('last-bank-selection-obp-flow')) {
        let bank = document.querySelector("#bank");
        bank.value = localStorage.getItem('last-bank-selection-obp-flow');
    }
};

window.addEventListener("load", (event) => {
  console.log("Page fully loaded");
  // The function to be executed
  restoreLastBankSelection();
});