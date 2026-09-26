package com.pickview.config;

import com.pickview.model.Account;
import com.pickview.model.Product;
import com.pickview.repository.IAccountRepository;
import com.pickview.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class DemoData implements CommandLineRunner {
    private final IAccountRepository mAccounts;
    private final IProductRepository mProducts;
    private final PasswordEncoder mEncoder;
    private final boolean mIsEnabled;
    private final String mPassword;

    public DemoData(IAccountRepository accounts, IProductRepository products, PasswordEncoder encoder,
                    @Value("${pickview.seed}") boolean isEnabled, @Value("${pickview.demo-password}") String password) {
        mAccounts = accounts; mProducts = products; mEncoder = encoder; mIsEnabled = isEnabled; mPassword = password;
    }

    @Override
    @Transactional
    public void run(String... arguments) {
        if (!mIsEnabled || mAccounts.count() != 0) { return; }
        createAccount("buyer", "BUYER", "나의 첫 Pick", "NONE");
        createAccount("seller", "BUYER", "스튜디오 온", "APPROVED");
        createAccount("admin", "ADMIN", "운영 관리자", "NONE");
        createAccount("content", "CONTENT", "콘텐츠 담당자", "NONE");
        createAccount("support", "SUPPORT", "고객지원 담당자", "NONE");
        createAccount("finance", "FINANCE", "정산 담당자", "NONE");
        createProduct("video-1", "내 손으로 만드는 첫 번째 도자기", "A small beginning: your first ceramic cup", "EDUCATION", "pottery", 3000);
        createProduct("video-2", "ETF, 시작하기 전에 알아야 할 것들", "Understand the basics before your first investment", "FINANCE", "finance", 2000);
        createProduct("video-3", "평범한 하루를 웃음으로 바꾸는 시간", "A little laughter for your everyday life", "COMEDY", "comedy", 1500);
        createProduct("video-4", "카메라 한 대로 시작하는 나의 스튜디오", "Build your creative space with one camera", "EDUCATION", "studio", 4000);
        createProduct("video-5", "일상을 기록하는 영상의 문법", "A thoughtful guide to everyday storytelling", "EDUCATION", "studio", 2500);
        createProduct("video-6", "돈을 이해하는 작은 습관", "Small habits for understanding money", "FINANCE", "finance", 0);
        mProducts.save(new Product("bundle-1", "seller", "처음 시작하는 크리에이터 패키지", "두 편으로 시작하는 창작. / Two videos to begin creating.",
                "EDUCATION", 5000, 30, "APPROVED", "studio", "", "", 60, "BUNDLE", "video-4,video-5", false, System.currentTimeMillis()));
    }

    private void createAccount(String id, String role, String name, String status) {
        mAccounts.save(new Account(id, id + "@pickview.demo", mEncoder.encode(mPassword), name, role, status,
                "매일의 작은 발견을 영상으로 전합니다. / Thoughtful videos for curious minds.", "ko", "EDUCATION"));
    }

    private void createProduct(String id, String title, String english, String category, String thumbnail, int price) {
        mProducts.save(new Product(id, "seller", title, english + "\n\n이 상품은 기능 시연용 샘플입니다. 재생 영상은 공통 데모 클립입니다. / Sample product with a shared demo clip.",
                category, price, 30, "APPROVED", thumbnail, "demo.mp4", "demo-preview.mp4", 30, "VIDEO", "", false, System.currentTimeMillis()));
    }
}
