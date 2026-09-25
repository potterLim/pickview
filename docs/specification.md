# PickView demo specification v1.0

## Scope
Video marketplace: Korean launch, Korean/English UI, KRW, individual and same-creator bundles; no subscription. Responsive web and native Expo iOS/Android clients share a Spring Boot server. Local development precedes external hosting. IntelliJ Ultimate is the reference IDE. All payments/refunds/settlements are simulated and never collect card or banking credentials.

## Buyer
Anonymous discovery, search/title/description/seller, category/rating filters and popularity/newest/rating sorting. Interest-based home, seller pages, wishlist/follow, private inquiries, verified purchaser reviews, cart, mock checkout, library, server-synced resume, notifications and settings. Purchase requires adult-confirmed account. Email/password authentication is real; social authentication is explicitly simulated.

## Catalog and access
Creators own rights, may sell elsewhere, apply for approval. Free or KRW >=1000 in increments of 100. Unlimited or 7/30/90 days starting at purchase; same prices across clients. Bundles share seller and period; block duplicate content owned or within cart. Sold bundle contents cannot be removed. Changes do not revoke historical access. Withdrawal preserves valid purchases; legal removal may disable playback with refund review. Expired purchases can be bought again. Existing buyers receive video updates.

## Media
Demo uploads: MP4 H.264/AAC, <=1080p, <=100MB, <=10min; validated server-side. Preview <=20% and <=60sec with distinct physical preview file, never expose full video through public preview. Server authorization for full playback, protected object storage. Seek, +/-10sec, .5–2x, fullscreen, cross-device resume. Concurrent devices allowed. No offline downloads/DRM. Large-file transcoding/adaptive streaming excluded.

## Seller
Personal/business application, channel profile, upload/title/description/thumbnail/category/price/period/preview/rights confirmation, optional tags, same-seller bundles. Draft/pending/approved/rejected/withdrawn. Review new videos and replacements. Metadata edits audited. Web/native authoring. Sales, fees, projected settlements and per-product figures. One owner per channel; no coupons/scheduling/teams/attachments.

## Orders and operations
Multi-seller cart, server-calculated totals, idempotent checkout, success/failure/cancel simulations. Platform 15% after configurable mock channel fee, seller 85%; show simulated rates. Monthly settlements on next-month 15th; >=KRW10000, smaller balances carry. Refund requests (unplayed cancellation, played reason review); approval atomically revokes grants and adjusts settlement; bundle refunds are whole bundle.

## Moderation
Separate ADMIN/CONTENT/SUPPORT/FINANCE permissions enforced server-side; auditable decisions and role changes. Reports and blocking. Prohibited pornography, hate, copyright infringement, illicit recordings, fraud and privacy abuse. In-app notifications, seller replies and support tickets; no actual email/push/chat.

## Acceptance
Seller upload -> approval -> cross-account discovery; mobile purchase -> desktop library; protected full playback; resume sync; withdrawal retention; expiry denial; refund/grant/ledger consistency; role and ownership isolation; idempotence/failure/cancel; responsive Korean/English. Source, migrations, seeds, setup, test reports, runnable web and test-build configuration. Native binaries require available signing/build infrastructure; report unverified platforms explicitly.

## Design
White #FFFFFF, ink #20202A, violet #7256E8, muted #747482, lavender #F4F1FE. Airy discovery with desktop sidebar, mobile bottom navigation, prominent thumbnails, price and term. Hero copy: 구독 없이, 보고 싶은 영상만. Screen inventory: discovery, details/player/reviews, seller profile, auth, cart/checkout, library, favorites/follows, orders/refunds, inbox/settings, seller application/editor/analytics, admin review/support/settlements. Design delegation was authorized; no further visual approval gate.
