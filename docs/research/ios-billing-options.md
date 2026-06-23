# EarAI — iOS Recurring-Billing Options for Users Inside Russia (2026)

**Scope:** How an iOS app "EarAI" (a voice‑AI subscription, 299–599 ₽/mo) can collect *recurring* payments from users physically inside the Russian Federation, given that standard StoreKit IAP is effectively dead for RF Apple Accounts.

**Date of research:** 2026‑06‑23. All "current behavior" claims verified via web search on this date.

---

## 0. The constraint that frames everything

As of **1 April 2026**, Apple disabled payment processing on the App Store and Apple Media Services in Russia, on the order of the RF Ministry of Digital Development. Concretely:

- New purchases, **in‑app purchases, and subscription renewals are unavailable** in Russia unless the user has a pre‑existing **Apple Account balance**.
- Users can still **redeem gift‑card / promo codes they already hold**, and spend down any remaining balance, but **cannot top up** (mobile‑operator top‑ups via MTS/Beeline/MegaFon/Tele2 were blocked from 1 April).
- If Apple cannot bill a renewal, **the subscription ends and access is lost**.

**Conclusion:** Native StoreKit auto‑renewable subscriptions are **not a viable billing rail** for RF‑resident users in 2026. The money has to move **outside** Apple's rails, and the iOS app's job shrinks to **authentication + entitlement unlock**. Every option below is judged on that basis.

Sources: [Apple Support — billing in Russia](https://support.apple.com/en-us/126891) · [9to5Mac](https://9to5mac.com/2026/04/02/apple-pulls-the-plug-on-all-payments-in-russia-following-government-diktat/) · [Macworld](https://www.macworld.com/article/3106031/iphone-users-in-russia-can-no-longer-buy-anything-apple-sells.html) · [Meduza — operator top‑ups blocked](https://meduza.io/en/news/2026/04/01/russia-blocks-apple-id-mobile-top-ups-amid-app-store-dispute)

---

## 1. The two App‑Store rules that decide your fate

Everything hinges on **which exemption your app claims** under Apple's App Review Guidelines, and on the **anti‑steering** rule. The current (2025–2026, post‑Epic) text:

### Guideline 3.1.1 — In‑App Purchase (the default obligation)
> "If you want to unlock features or functionality within your app … you must use in‑app purchase. Apps may not use their own mechanisms to unlock content or functionality, such as license keys … QR codes, cryptocurrencies …"

### Guideline 3.1.1(a) / anti‑steering — the part that hurts RF
> "In all other storefronts, **except for the United States storefront**, … apps and their metadata **may not include buttons, external links, or other calls to action that direct customers to purchasing mechanisms other than in‑app purchase.**"

External‑purchase **links/buttons are only allowed in the US storefront (no entitlement) and in the EU (via the StoreKit External Purchase Link Entitlement under DMA).** The allowed‑regions list for the custom‑purchase‑link entitlement is **EU/EEA member states only**. **The Russian storefront is not on any external‑link list.** So inside an app served to the RF storefront, you may **not** show a "Subscribe on our website" button or a price + link.

### Guideline 3.1.3(a) — "Reader" apps (the loophole that *can* survive review)
> "Apps may allow a user to **access previously purchased content or content subscriptions** … Reader apps may offer **account creation for free tiers, and account management functionality for existing customers**."

A reader app may **let the user sign in** and unlock content they bought elsewhere, **without** offering IAP — and (outside the US) **without** any link or call‑to‑action pointing at the external purchase. The catch: the historical "reader" category is enumerated as *magazines, newspapers, books, audio, music, and video*. A pure voice‑AI tool is a gray‑area fit (see §11 uncertainties), but the **mechanic** Apple actually enforces — "log in to access what you bought elsewhere, don't mention where/how to buy" — is the same one Spotify, Netflix, Kindle, and ChatGPT's iOS app rely on and is routinely approved.

### Guideline 3.1.3(b) — Multiplatform Services (a trap, not a help)
> "Apps that operate across multiple platforms may allow users to access content … they have acquired … on … your web site, **provided those items are also available as in‑app purchases within the app.**"

3.1.3(b) **requires IAP parity** — you'd have to *also* offer the sub via IAP. Since IAP is broken in RF, citing 3.1.3(b) is self‑defeating. **Do not build to 3.1.3(b); build to 3.1.3(a).**

Sources: [App Review Guidelines (live)](https://developer.apple.com/app-store/review/guidelines/) · [9to5Mac — May 2025 external‑links update](https://9to5mac.com/2025/05/01/apple-app-store-guidelines-external-links/) · [Apple — reader apps support](https://developer.apple.com/support/reader-apps/) · [custom‑purchase‑link allowed‑regions (EU list)](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.storekit.custom-purchase-link.allowed-regions) · [3.1.3(b) forum discussion](https://developer.apple.com/forums/thread/117077)

---

## 2. Channel‑by‑channel analysis

### Channel A — Web checkout + "reader app" sign‑in (the workhorse)

**How it works on iOS:** EarAI publishes a free iOS app. The user pays on **earai.ru** (YooKassa / CloudPayments) — card, SberPay, **СБП**, T‑Pay, YuMoney wallet. The website sets up a **recurring** charge (card‑token autopayment or СБП‑подписка). The backend marks the account "active." In the app the user taps **"Войти" (Sign in)**, authenticates, and the entitlement unlocks. The app never shows a price or a "buy" button.

- **App‑Store compliant?** Yes, under **3.1.3(a)** *if* you treat it as account‑access‑only and stay silent on payment. The act of letting an existing customer sign in and use what they bought elsewhere is explicitly allowed. Account creation for a free tier is allowed. The **anti‑steering line is the tightrope**: outside the US storefront you may not link to or advertise the external purchase inside the app. (See §3 for exact wording.)
- **RF‑payable?** Yes — fully domestic. YooKassa (Sber) and CloudPayments (T‑Bank) support cards/Mir, СБП, SberPay, T‑Pay, YuMoney with **recurring billing** and **54‑ФЗ** receipts. СБП‑подписки (account‑linked recurring debits) are live and bank‑managed.
- **UX friction:** Medium. Steps: open site (or scan QR from app's help screen) → choose plan → pay via СБП/card → confirm recurring → return to app → sign in. ~5–7 taps, and a **context switch out of the app** (Safari/bank app). First renewal is automatic thereafter.
- **Fees / commission:** **0% to Apple.** YooKassa cards/SberPay/YuMoney ~2.5% promo (28.01–01.12.2026), autopayments from ~2.8%; СБП ~0.4–0.7%. CloudPayments СБП 0.7%, cards 3.4% (→2.7% >1M ₽/mo). 54‑ФЗ receipt fee at YooKassa ~0.8% (1.5% for СБП), min 65 коп.
- **Fiscalization (54‑ФЗ):** Native. Both providers auto‑issue cloud receipts; note the **VAT base rate rose 20%→22% on 1 Jan 2026** — providers already support it.
- **Rejection risk:** **Low–Medium.** Low if the app is genuinely sign‑in‑only with no payment UI/links and no "to subscribe, go to…" copy. Medium because (a) reviewers sometimes mis‑apply 3.1.1 to non‑classic‑reader apps, and (b) the "reader" enumeration doesn't list "AI tools" — a reviewer could argue IAP is required. Mitigations: keep zero purchase affordances in‑app; have a clean "this is account access for existing customers" reply ready.

Sources: [Apple reader‑apps](https://developer.apple.com/support/reader-apps/) · [YooKassa fees](https://yookassa.ru/fees/) · [YooKassa autopayments](https://yookassa.ru/regulyarnye-platezhi/) · [CloudPayments](https://cloudpayments.ru/) · [СБП подписки (NSPK)](https://sbp.nspk.ru/blog/kak-ispolzovat-sbp-dlia-oplaty-podpisok) · [3.1.1 rejection guide](https://iossubmissionguide.com/guideline-3-1-in-app-purchase/)

---

### Channel B — Telegram bot sells the sub → backend unlocks iOS (the RF‑native power play)

This splits into three sub‑mechanisms with **very different** economics.

#### B1. Telegram Bot Payments API + Russian provider (YooKassa/CloudPayments) — card/СБП
**How it works:** A `@EarAIBot` collects payment via the Bot Payments API wired to **YooKassa or CloudPayments**. Backend links the Telegram user → EarAI account → iOS entitlement.

- **The Telegram catch (2025 rule):** *"Payments for digital goods and services [inside Telegram apps] must be carried out **exclusively in Telegram Stars** … transactions for digital goods cannot be processed through third‑party payment providers."* Telegram will **hide your bot/mini‑app from mobile users** if you try to sell digital goods via YooKassa cards. **Third‑party providers are allowed only for physical goods/services.** This rule exists *because* of Apple/Google store policy.
- **Practical consequence:** You **cannot** sell the EarAI digital subscription via a YooKassa flow *shown inside the Telegram mobile app*. The compliant pattern is: the bot hands the user a **deep link to the EarAI website checkout** (opened in the system browser, not Telegram's in‑app view) — at which point this collapses into **Channel A** with Telegram as the marketing/entry funnel.
- **RF‑payable / fees / 54‑ФЗ:** Same as Channel A (YooKassa/CloudPayments). Telegram adds **0%** on top.
- **Rejection risk (App Store):** The iOS app still only signs in → same 3.1.3(a) posture. **Do not** mention "pay in the bot" inside the iOS app.

#### B2. Telegram Stars (digital‑goods‑native) — **Apple takes 30%**
**How it works:** Bot sells the sub for **Stars**; user buys Stars via Apple IAP inside Telegram's iOS app.

- **Apple's cut:** When Stars are purchased **inside Telegram's iOS app**, **Apple takes its ~30% IAP commission** on that Star purchase. Telegram is itself an iOS app subject to StoreKit. **Also — and decisively for RF — buying Stars via Apple IAP needs a working Apple Account balance, which RF users no longer have (see §0).** So Stars‑via‑iOS is **both expensive and largely unavailable** to your RF users in 2026.
- **Workaround:** Stars bought via **@PremiumBot / Fragment / TON** off the Apple rail avoid the 30% — but that's a crypto‑adjacent top‑up flow with real UX friction and its own RF‑payment questions. Star→TON withdrawal carries a 21‑day hold; 1 Star ≈ $0.013–0.015.
- **RF‑payable:** Weak. The clean Stars path runs through Apple IAP (dead in RF); the alternative is TON/Fragment.
- **Verdict:** Stars is the *only* Telegram‑compliant way to sell a *digital* sub **inside** Telegram, but the Apple‑IAP dependency makes it **the wrong rail for RF in 2026.** Useful only for non‑RF users or as a tip jar.

#### B3. Telegram Mini App payments
Same rule as B1/B2: **digital goods inside a Mini App must use Stars.** A Mini App can host the *experience*, but to take card/СБП money for a digital sub it must **send the user out to the web checkout** (Channel A). Mini Apps may use third‑party providers only for **physical** goods.

- **UX friction (Telegram overall):** **Lowest of all channels** for the *payment moment* if you route via the website checkout from a bot button — RF users live in Telegram, СБП/card autofill is fast, ~3–4 taps. The only friction is the browser hop and the account‑link step.
- **Fiscalization:** Via YooKassa/CloudPayments → 54‑ФЗ handled.

Sources: [Telegram — Bot Payments for Digital Goods (Stars rule)](https://core.telegram.org/bots/payments-stars) · [GramBase — Telegram payments 2026](https://grambase.ai/blog/telegram-payments-guide-2026) · [Telegram Stars blog](https://telegram.org/blog/telegram-stars) · [Mava — Stars & Apple 30%](https://www.mava.app/blog/telegram-stars-telegrams-in-app-currency) · [Cointelegraph — Stars launch](https://cointelegraph.com/news/telegram-unveils-stars-token-for-in-app-purchases)

---

### Channel C — Apple External Purchase Link Entitlement (StoreKit) — **not available in RF**
**How it works (where allowed):** App shows a real "buy on our site" button/link; Apple takes a reduced commission (US: 27%, or 12% small‑business, on external‑link‑originated purchases).

- **RF availability:** **No.** External purchase links are permitted **only on the US storefront** (no entitlement) and in the **EU** (DMA entitlement; allowed‑regions = EU/EEA only). **The Russian storefront is on no external‑link list.** There is no DMA‑equivalent forcing Apple to open this in RF.
- **Does EU/US help RF?** Only if a user's **storefront** is US/EU (i.e., a non‑RF Apple Account). That's not your RF‑resident target user. **Irrelevant for an RF‑only launch.**
- **Verdict:** **Unusable in RF in 2026.** Listed for completeness.

Sources: [External Purchase docs](https://developer.apple.com/documentation/storekit/external-purchase) · [custom‑purchase‑link allowed‑regions (EU only)](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.storekit.custom-purchase-link.allowed-regions) · [Apple — EU offer communication](https://developer.apple.com/support/communication-and-promotion-of-offers-on-the-app-store-in-the-eu/)

---

### Channel D — Other RF messengers / wallets (VK, SberPay, YuMoney, T‑Pay, MAX)
These are **payment methods/funnels, not separate iOS billing rails.** They all reduce to "user pays on the web/in a super‑app, backend unlocks iOS."

- **VK / VK Pay / MAX super‑app:** VK supports payments to merchants/communities; the state‑backed **MAX** super‑app bundles chats + payments + store + digital ID. Either can be a **marketing/checkout funnel** that ends in a web payment + account link. No special App‑Store treatment; same 3.1.3(a) posture in‑app.
- **SberPay / YuMoney / T‑Pay:** These are **payment instruments already inside YooKassa & CloudPayments** — you get them for free in Channel A. Recurring supported; 54‑ФЗ supported.
- **RF‑payable:** Yes (all domestic). **Fees:** as Channel A. **Rejection risk:** same as A (the iOS app only signs in).
- **Verdict:** Treat as **payment options within Channel A**, plus VK/MAX as an *optional* second acquisition funnel. Not a distinct strategy.

Sources: [thePaypers — VK merchant payments](https://thepaypers.com/payments/news/russia-vk-now-supports-payments-to-e-merchants-online-communities) · [russiable — MAX app](https://russiable.com/max-app-russia/) · [YooKassa SDK payment methods](https://yookassa.ru/developers/using-api/using-sdks)

---

### Channel E — Gift cards / promo codes / cross‑platform entitlement (buy on Android/RuStore/web → use on iOS)
**How it works:** Sell the sub where money flows freely — **web, or Android via RuStore/direct APK** — and grant the entitlement to the same account; the iOS app just signs in.

- **RuStore on iOS:** **Not an app store on iPhone.** The 2025 RF law mandating RuStore pre‑install on iPhones does **not** give iOS alternative‑store/APK installs (iOS doesn't allow it). So RuStore is an **Android‑only purchase channel** whose entitlement you mirror to iOS via your backend.
- **Cross‑platform entitlement:** Standard pattern — backend maps platform product IDs → one unified entitlement; iOS reads entitlement, doesn't sell. This is exactly the 3.1.3(a) sign‑in model again.
- **Apple gift cards / promo codes:** Largely **dead in RF** — users can't top up Apple balances, and you can't realistically distribute Apple promo codes for a recurring sub at scale. **Not a recurring rail.**
- **RF‑payable:** Web/Android = yes (YooKassa/CloudPayments/RuStore billing). **Fees:** web as Channel A; RuStore billing has its own commission.
- **Rejection risk:** Low for the iOS side (sign‑in only). **Do not** advertise the Android/web purchase inside the iOS app (anti‑steering).
- **Verdict:** Strong **fallback/complement** — capture Android users natively, mirror to iOS. Doesn't solve the *iOS‑first* user's payment moment by itself.

Sources: [RuStore (Wikipedia)](https://en.wikipedia.org/wiki/RuStore) · [Izvestia — RuStore on iPhone explained](https://en.iz.ru/en/1914421/2025-07-02/analyst-explained-principle-installing-rustore-iphone) · [RevenueCat — cross‑platform entitlements](https://www.revenuecat.com/blog/engineering/cross-platform-subscription/)

---

## 3. The "reader‑app tightrope" — exact in‑app wording & flow

**Goal:** unlock for paying users without (a) using IAP or (b) steering to external payment (forbidden outside the US storefront).

**ALLOWED in‑app (RF storefront):**
- A **"Войти / Sign in"** button and login/registration fields (free‑tier account creation is permitted).
- Neutral status copy: *"Подписка не активна"* / *"Subscription inactive."*
- Generic account management: *"Управление аккаунтом"* that, post‑login, may show subscription **status** (not a buy button).
- A free trial / limited free tier fully inside the app (no payment).

**FORBIDDEN in‑app (RF storefront):**
- Any **"Subscribe," "Buy," "Upgrade," price tag, or "Go to our website to pay"** button or text.
- Any **link/QR/deep‑link to the checkout** shown as a call‑to‑action. (A QR in a *support/help* context is risky — treat any purchase pointer as forbidden.)
- Copy that *discourages* IAP or *names* the cheaper external method.

**Where to put the purchase prompt instead (compliant):**
- **Outside the app:** email, push from your own server, the website itself, the Telegram bot, VK/MAX. Apple explicitly permits **out‑of‑app communications** about other purchase methods: *"Developers can send communications outside of the app to their user base about purchasing methods other than in‑app purchase."*
- New users discover pricing on **earai.ru** or via the **bot**, pay there, then return and sign in.

**Recommended copy for an unauthenticated/inactive user:**
> "Войдите, чтобы продолжить. Нет аккаунта? Зарегистрируйтесь бесплатно." *(Sign in to continue. No account? Register for free.)*
> *(No price, no "subscribe," no external link.)*

If a reviewer asks how users pay: answer that **EarAI is a multi‑platform service and the iOS app provides account access for existing customers** (3.1.3(a) framing). Do **not** volunteer that it's cheaper on the web.

Source: [App Review Guidelines 3.1.3 / 3.1.1(a)](https://developer.apple.com/app-store/review/guidelines/)

---

## 4. Ranked comparison table

| Rank | Channel | iOS mechanic | App‑Store compliant? (guideline) | RF‑payable? | UX friction (subscribe) | Fee to Apple | Provider fee + 54‑ФЗ | Rejection risk |
|---|---|---|---|---|---|---|---|---|
| **1** | **A. Web checkout + reader sign‑in** | App = sign‑in only; pay on earai.ru (СБП/card/SberPay/T‑Pay), recurring | ✅ 3.1.3(a) if no in‑app payment UI/links | ✅ YooKassa/CloudPayments | Medium (browser hop + login) | **0%** | СБП 0.4–0.7%, cards ~2.5–3.4%, autopay ~2.8%; 54‑ФЗ native | Low–Med |
| **2** | **B1/B3. Telegram bot/Mini App → web checkout** | Bot funnels to web pay; app signs in | ✅ 3.1.3(a) in‑app; Telegram‑OK (sale happens on web, not inside TG) | ✅ same providers | **Low** (TG‑native, ~3–4 taps) | **0%** | same as A | Low–Med |
| **3** | **E. Android/RuStore/web purchase → iOS entitlement mirror** | Buy on Android/web; iOS signs in | ✅ 3.1.3(a) iOS side | ✅ web/RuStore | Low for Android‑first users; N/A for iOS‑first | 0% (iOS); RuStore billing fee on Android | provider 54‑ФЗ | Low |
| **4** | **D. VK / MAX / wallet funnels** | Pay in super‑app/web; app signs in | ✅ 3.1.3(a) | ✅ | Medium | 0% | as A | Low–Med |
| 5 | **B2. Telegram Stars (digital‑native)** | Buy Stars (Apple IAP) → spend on bot | ✅ Telegram rule; ⚠️ uses Apple IAP | ❌ Stars via iOS IAP needs Apple balance (dead in RF); TON path only | High (Stars/TON top‑up) | **~30%** (Apple, when via iOS) | n/a cleanly | n/a |
| 6 | **C. Apple External Purchase Link** | Real "buy on web" button in‑app | ✅ but **US/EU storefronts only** | ❌ not on RF storefront | Low (if it existed) | 27% / 12% | n/a | n/a in RF |

---

## 5. Opinionated recommendation (RF‑only launch)

**PRIMARY: Channel A — Web checkout + "reader‑app" sign‑in, with СБП‑подписка as the default recurring method.**
- Free iOS app, **sign‑in only**, zero purchase UI. Entitlement comes from the backend.
- Checkout on **earai.ru** via **YooKassa** (broadest method coverage: СБП, cards/Mir, SberPay, T‑Pay, YuMoney; native 54‑ФЗ). Make **СБП‑подписка** the default recurring rail — lowest fee (~0.4–0.7%), no card data, bank‑managed cancellation, best RF trust. Keep **card‑token autopayment** as a fallback for users whose bank's СБП autopay is clunky.
- This is the only path that is simultaneously **App‑Store‑survivable (3.1.3(a))**, **fully RF‑payable**, **0% to Apple**, and **54‑ФЗ‑clean**.

**FALLBACK / co‑primary acquisition funnel: Channel B1/B3 — Telegram bot as the front door.**
- `@EarAIBot` is where RF users *discover and start* the sub. The bot **must not** sell the digital sub via YooKassa inside Telegram (Stars‑only rule) — instead it presents a **"Оформить подписку"** button that opens the **earai.ru checkout in the system browser**, then deep‑links back and auto‑links the Telegram identity to the EarAI account. This gives the **lowest‑friction payment moment** for the TG‑heavy RF audience while staying inside both Telegram's and Apple's rules.
- Telegram also doubles as your **out‑of‑app communication channel** (renewal reminders, win‑back) that Apple explicitly permits.

**SECONDARY (capture, don't depend on): Channel E.** Ship an **Android/RuStore** build with native purchase, mirror the entitlement to iOS. Converts the Android slice at lower friction and feeds the same account system.

**Avoid:** Telegram Stars (B2) for RF billing (Apple‑IAP‑dependent, ~30%, and the Apple balance is dead in RF) and Apple External Purchase Links (C, not available on the RF storefront). Keep Stars only as an optional **tip jar** or for non‑RF users.

**The one rule that keeps you alive at review:** the iOS app, on the RF storefront, must contain **no price, no "subscribe/buy," and no link or QR to the checkout** — only **"Войти / Register free"** and neutral status text. All pricing and payment lives on the **web and in the bot**, reached by users **on their own**, never via an in‑app call‑to‑action.

---

## 6. Implementation checklist (PRIMARY path)

1. Backend: unified **account + entitlement** service (email/phone/Telegram identity → `active/inactive` + expiry). Single source of truth for all platforms.
2. Web checkout on earai.ru: **YooKassa** with СБП‑подписка (default) + card autopayment (fallback); 54‑ФЗ receipts on.
3. iOS app: free, **sign‑in only**, no purchase affordances; reads entitlement; neutral "inactive" state; clean reviewer‑facing rationale ("multi‑platform service; iOS = account access for existing customers").
4. Telegram bot: discovery + **"Оформить подписку"** → opens web checkout in system browser → links identity back. **No in‑Telegram digital sale.** Use bot/email/push for renewals.
5. Optional: Android/RuStore build mirroring the same entitlement.
6. Monitor first renewals (СБП autopay + card token); build grace‑period + dunning via bot/email.

---

## 7. Flagged uncertainties / things to verify before betting the launch

1. **Reader‑app category fit for a "voice‑AI tool."** 3.1.3(a)'s enumerated categories are media (magazines/books/audio/music/video), not "AI assistant." Apple's *enforced* mechanic (sign‑in to access prior purchases, no steering) is what matters, and AI/SaaS companion apps with web‑only billing are regularly approved — but **a reviewer could insist on IAP under 3.1.1.** **Verify** by reading current rejection threads for AI/SaaS companion apps and, ideally, a pre‑submission query to App Review. Risk is real but manageable; have the 3.1.3(a) account‑access argument ready.
2. **Does Apple specifically tolerate sign‑in‑only billing on the *RF storefront* given the April‑2026 lockout?** Logically yes (Apple itself broke IAP in RF, so it cannot demand IAP), but I found **no explicit Apple statement** that it relaxes 3.1.1 enforcement for RF apps. **Verify** with a current RF developer or App Review.
3. **Telegram Stars + Apple 30% exact mechanics in 2026.** Confirmed that Stars bought via iOS incur Apple's IAP commission and that digital goods in Telegram must use Stars; the **precise** current commission and whether RF users can buy Stars at all post‑April‑2026 (no Apple balance) should be **re‑verified** — my read is RF users effectively can't, pushing them to TON/Fragment.
4. **Exact 2026 commission cards for YooKassa/CloudPayments** depend on merchant category, turnover, and negotiated rates. Figures here are list/promo rates (YooKassa promo ~2.5% to 01.12.2026; СБП ~0.4–0.7%; autopay from ~2.8%; CloudPayments СБП 0.7%, cards 3.4%→2.7%). **Get a written quote.**
5. **СБП‑подписка coverage across banks.** Recurring СБП is live and bank‑managed, but **not every RF bank** exposes a smooth autopay‑binding UX. Keep card‑token autopayment as a universal fallback.
6. **MAX super‑app mandate.** RF is pushing the state‑backed MAX super‑app (and RuStore pre‑install). Worth watching as a future low‑friction RF billing funnel, but **immature** as of mid‑2026 — don't build on it yet.
7. **VAT 22% (from 1 Jan 2026).** Confirm your 54‑ФЗ receipt template and pricing reflect the new base rate.

---

## 8. Sources

- Apple Support — [Billing for Apple subscriptions/digital purchases in Russia](https://support.apple.com/en-us/126891)
- [9to5Mac — Apple pulls payments in Russia (Apr 2026)](https://9to5mac.com/2026/04/02/apple-pulls-the-plug-on-all-payments-in-russia-following-government-diktat/)
- [Macworld — iPhone users in Russia can't buy](https://www.macworld.com/article/3106031/iphone-users-in-russia-can-no-longer-buy-anything-apple-sells.html)
- [Meduza — RF blocks Apple ID top‑ups](https://meduza.io/en/news/2026/04/01/russia-blocks-apple-id-mobile-top-ups-amid-app-store-dispute)
- [Apple App Review Guidelines (live)](https://developer.apple.com/app-store/review/guidelines/)
- [9to5Mac — May 2025 external‑links guideline update](https://9to5mac.com/2025/05/01/apple-app-store-guidelines-external-links/)
- [Apple — Distributing reader apps with a website link](https://developer.apple.com/support/reader-apps/)
- [Apple — custom‑purchase‑link allowed‑regions (EU only)](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.storekit.custom-purchase-link.allowed-regions)
- [Apple — External Purchase docs](https://developer.apple.com/documentation/storekit/external-purchase)
- [Apple — EU offer communication/promotion](https://developer.apple.com/support/communication-and-promotion-of-offers-on-the-app-store-in-the-eu/)
- [Apple Dev Forums — 3.1.3(b) multiplatform](https://developer.apple.com/forums/thread/117077)
- [Telegram — Bot Payments for Digital Goods (Stars‑only rule)](https://core.telegram.org/bots/payments-stars)
- [Telegram Stars blog](https://telegram.org/blog/telegram-stars)
- [GramBase — Telegram payments guide 2026](https://grambase.ai/blog/telegram-payments-guide-2026)
- [Mava — Telegram Stars & Apple 30%](https://www.mava.app/blog/telegram-stars-telegrams-in-app-currency)
- [Cointelegraph — Telegram Stars launch](https://cointelegraph.com/news/telegram-unveils-stars-token-for-in-app-purchases)
- [YooKassa — fees](https://yookassa.ru/fees/) · [YooKassa — recurring/autopayments](https://yookassa.ru/regulyarnye-platezhi/) · [YooKassa — 54‑ФЗ basics](https://yookassa.ru/developers/payment-acceptance/receipts/54fz/basics)
- [CloudPayments](https://cloudpayments.ru/) · [a2is — CloudPayments vs YooKassa 2026](https://a2is.ru/catalog/platyozhnye-sistemy/compare/cloudpayments/yandex-kassa)
- [NSPK СБП — paying for subscriptions](https://sbp.nspk.ru/blog/kak-ispolzovat-sbp-dlia-oplaty-podpisok) · [СБП autopayment](https://sbp.nspk.ru/blog/kak-rabotaet-avtoplatez-cerez-sbp)
- [thePaypers — VK merchant payments](https://thepaypers.com/payments/news/russia-vk-now-supports-payments-to-e-merchants-online-communities) · [russiable — MAX app](https://russiable.com/max-app-russia/)
- [RuStore — Wikipedia](https://en.wikipedia.org/wiki/RuStore) · [Izvestia — RuStore on iPhone](https://en.iz.ru/en/1914421/2025-07-02/analyst-explained-principle-installing-rustore-iphone)
- [RevenueCat — cross‑platform entitlements](https://www.revenuecat.com/blog/engineering/cross-platform-subscription/)
- [iOS submission guide — 3.1.1 rejections](https://iossubmissionguide.com/guideline-3-1-in-app-purchase/)
