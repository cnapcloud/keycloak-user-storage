package com.keycloak.userstorage.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;

import com.keycloak.userstorage.model.User;

@Slf4j
public class UserRepositoryImpl implements UserQueryRepository {

    private static final Set<String> SKIP_KEYS = Set.of("first", "max", "search");

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<User> findUsersByConditions(Map<String, Object> params, Integer first, Integer max) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        List<Predicate> predicates = buildPredicates(cb, root, query, params);

        query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
        TypedQuery<User> typedQuery = entityManager.createQuery(query);

        if (first != null) typedQuery.setFirstResult(first);
        if (max != null) typedQuery.setMaxResults(max);

        return typedQuery.getResultList();
    }

    @Override
    public Long countUsersByConditions(Map<String, Object> params) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> root = query.from(User.class);

        List<Predicate> predicates = buildPredicates(cb, root, query, params);
        query.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(query).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<User> root, CriteriaQuery<?> query, Map<String, Object> params) {
        List<Predicate> predicates = new ArrayList<>();
        params.forEach((key, value) -> {
            if (SKIP_KEYS.contains(key)) return;
            try {
                root.get(key); // 직접 필드 존재 확인
                predicates.add(cb.like(root.<String>get(key), "%" + value + "%"));
            } catch (IllegalArgumentException e) {
                // attributes 키 → USER_ATTRIBUTES JOIN EXISTS 서브쿼리
                Subquery<String> sub = query.subquery(String.class);
                Root<User> subRoot = sub.from(User.class);
                MapJoin<User, String, String> attrJoin = subRoot.joinMap("attributes");
                sub.select(subRoot.get("id"))
                   .where(
                       cb.equal(subRoot.get("id"), root.get("id")),
                       cb.equal(attrJoin.key(), key),
                       cb.like(attrJoin.value(), "%" + value + "%")
                   );
                predicates.add(cb.exists(sub));
            }
        });
        return predicates;
    }

    @Override
    public List<User> searchUserWithAllFields(String search, Integer first, Integer max) {
        String jpql = "SELECT p FROM User p WHERE p.username LIKE :search OR p.firstName LIKE :search OR p.lastName LIKE :search";
        TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
        query.setParameter("search", "%" + search + "%");
        if (first != null) query.setFirstResult(first);
        if (max != null) query.setMaxResults(max);
        return query.getResultList();
    }

    @Override
    public Long countUsersWithWithAllFields(String search) {
        String jpql = "SELECT COUNT(p) FROM User p WHERE p.username LIKE :search OR p.firstName LIKE :search OR p.lastName LIKE :search";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("search", "%" + search + "%");
        return query.getSingleResult();
    }
}
