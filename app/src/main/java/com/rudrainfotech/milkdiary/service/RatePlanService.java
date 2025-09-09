package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.*;
import org.hibernate.Session;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RatePlanService {

    public List<RatePlan> listByOutlet(Outlet outlet) {
        return Tx.tx((Session s) ->
            s.createQuery("from RatePlan p where p.outlet=:o order by p.startDate desc, p.id desc", RatePlan.class)
             .setParameter("o", outlet)
             .getResultList()
        );
    }

    public RatePlan findActivePlan(Outlet outlet, LocalDate onDate) {
        return Tx.tx((Session s) ->
            s.createQuery("""
                    from RatePlan p
                    where p.outlet=:o and p.startDate <= :d and (p.endDate is null or p.endDate >= :d)
                    order by p.startDate desc, p.id desc
                    """, RatePlan.class)
             .setParameter("o", outlet)
             .setParameter("d", onDate)
             .setMaxResults(1)
             .uniqueResult()
        );
    }

    public RatePlan save(RatePlan plan) {
        // validate dates & overlap against other plans
        validateDateRange(plan);
        validateNoPlanOverlap(plan);

        // normalize sortOrder per species
        plan.getItems().stream().filter(it -> it.getSpecies()==Species.COW)
             .sorted(Comparator.comparing(it -> it.getSortOrder()==null?0:it.getSortOrder()))
             .forEachOrdered(new java.util.function.Consumer<RateItem>() {
                 int i=0; @Override public void accept(RateItem t){ t.setSortOrder(i++); }
             });
        plan.getItems().stream().filter(it -> it.getSpecies()==Species.BUFFALO)
             .sorted(Comparator.comparing(it -> it.getSortOrder()==null?0:it.getSortOrder()))
             .forEachOrdered(new java.util.function.Consumer<RateItem>() {
                 int i=0; @Override public void accept(RateItem t){ t.setSortOrder(i++); }
             });

        // slab overlap check inside a transaction so we can look only at in-memory list
        RatePlanValidator.ensureNoOverlappingSlabs(plan);

        return Tx.tx((Session s) -> s.merge(plan)); // cascade takes care of items
    }

    public void delete(RatePlan plan) {
        Tx.tx((Session s) -> {
            RatePlan managed = s.find(RatePlan.class, plan.getId());
            if (managed != null) s.remove(managed);
            return null;
        });
    }

    public RatePlan clonePlan(RatePlan original, String newName, LocalDate newStart, LocalDate newEnd) {
        return Tx.tx((Session s) -> {
            RatePlan src = s.find(RatePlan.class, original.getId());
            if (src == null) throw new IllegalArgumentException("Plan not found");

            RatePlan copy = new RatePlan();
            copy.setOutlet(src.getOutlet());
            copy.setName(newName);
            copy.setStartDate(newStart);
            copy.setEndDate(newEnd);

            List<RateItem> items = new ArrayList<>();
            for (RateItem it : src.getItems()) {
                RateItem c = new RateItem();
                c.setPlan(copy);
                c.setSpecies(it.getSpecies());
                c.setType(it.getType());
                c.setMinFat(it.getMinFat()); c.setMaxFat(it.getMaxFat());
                c.setMinSnf(it.getMinSnf()); c.setMaxSnf(it.getMaxSnf());
                c.setRatePerLitre(it.getRatePerLitre());
                c.setBase(it.getBase()); c.setPerFat(it.getPerFat()); c.setPerSnf(it.getPerSnf());
                c.setSortOrder(it.getSortOrder());
                items.add(c);
            }
            copy.getItems().addAll(items);
            s.persist(copy);
            return copy;
        });
    }

    private static void validateDateRange(RatePlan p) {
        if (p.getStartDate()==null) throw new IllegalArgumentException("Start date is required");
        if (p.getEndDate()!=null && p.getEndDate().isBefore(p.getStartDate()))
            throw new IllegalArgumentException("End date cannot be before start date");
    }

    private void validateNoPlanOverlap(RatePlan p) {
        // Validate dates first
        if (p.getStartDate() == null) throw new IllegalArgumentException("Start date is required");
        if (p.getEndDate() != null && p.getEndDate().isBefore(p.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        // Use a typed fallback for open-ended plans
        java.time.LocalDate pend = (p.getEndDate() != null)
                ? p.getEndDate()
                : java.time.LocalDate.of(9999, 12, 31);

        var overlaps = Tx.tx(s -> s.createQuery("""
            from RatePlan x
            where x.outlet = :o
              and (:id is null or x.id <> :id)
              and x.startDate <= :pend
              and (x.endDate is null or x.endDate >= :pstart)
            """, RatePlan.class)
                .setParameter("o", p.getOutlet())
                .setParameter("id", p.getId())
                .setParameter("pstart", p.getStartDate())
                .setParameter("pend", pend)
                .getResultList()
        );

        if (!overlaps.isEmpty()) {
            String msg = overlaps.stream()
                    .map(x -> x.getName() + " [" + x.getStartDate() + ".." + x.getEndDate() + "]")
                    .reduce("Overlaps existing plan(s): ", (a,b) -> a + b + "; ");
            throw new IllegalArgumentException(msg);
        }
    }

    public RatePlan findWithItems(Long id) {
        return Tx.tx(s -> s.createQuery("""
            select distinct p
            from RatePlan p
            left join fetch p.items i
            where p.id = :id
            """, RatePlan.class)
                .setParameter("id", id)
                .uniqueResult()
        );
    }

    public RatePlan findActivePlanWithItems(Outlet outlet, java.time.LocalDate onDate) {
        return Tx.tx(s -> {
            RatePlan p = s.createQuery("""
                from RatePlan p
                where p.outlet=:o and p.startDate <= :d and (p.endDate is null or p.endDate >= :d)
                order by p.startDate desc, p.id desc
                """, RatePlan.class)
                    .setParameter("o", outlet)
                    .setParameter("d", onDate)
                    .setMaxResults(1)
                    .uniqueResult();
            if (p != null) {
                // initialize items while the Session is still open
                p.getItems().size();
            }
            return p;
        });
    }
}
