package com.example.nutriuniv.domain.pns.repository;

import com.example.nutriuniv.domain.pns.entity.ProductPnsByEer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductPnsByEerRepository
        extends JpaRepository<ProductPnsByEer, ProductPnsByEer.PnsId>,
                ProductPnsByEerCustom {

    @Modifying
    @Query("DELETE FROM ProductPnsByEer p WHERE p.eerBand = :band")
    int deleteByEerBand(int band);
}

interface ProductPnsByEerCustom {
    /** product → category(depth=2) → category(depth=1) parent_id 매핑. */
    List<Object[]> fetchProductsWithParentCategory();
}

@RequiredArgsConstructor
class ProductPnsByEerCustomImpl implements ProductPnsByEerCustom {

    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Object[]> fetchProductsWithParentCategory() {
        // 반환: [product_id, parent_category_id,
        //        carb, protein, fat, fiber, cholesterol,
        //        satFat, transFat, sugar, sodium]
        String sql = """
            SELECT p.id,
                   c.parent_id,
                   pn.carbohydrate, pn.protein, pn.fat,
                   pn.dietary_fiber, pn.cholesterol,
                   pn.saturated_fat, pn.trans_fat,
                   pn.sugar, pn.sodium
            FROM   products p
            JOIN   categories c        ON p.category_id = c.id
            JOIN   product_nutrients pn ON pn.product_id = p.id
            WHERE  p.is_active = TRUE
              AND  c.parent_id IS NOT NULL
              AND  pn.calories IS NOT NULL
            """;
        return em.createNativeQuery(sql).getResultList();
    }
}
