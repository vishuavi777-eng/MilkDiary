package com.rudrainfotech.milkdiary.service;

import com.rudrainfotech.milkdiary.entity.Outlet;
import org.hibernate.Session;

import java.util.List;

public class OutletService {

    public Outlet getDefaultOutlet() {
        return Tx.tx((Session s) ->
                s.createQuery("from Outlet o order by o.id asc", Outlet.class)
                        .setMaxResults(1)
                        .uniqueResult()
        );
    }

    public Outlet getActiveOutlet() {
        AppSettingsService st = new AppSettingsService();
        Long id = st.getActiveOutletId();
        if (id != null) {
            Outlet o = Tx.tx((Session s) -> s.find(Outlet.class, id));
            if (o != null) return o;
        }
        // fallback to first outlet & ensure saved as active
        Outlet first = Tx.tx((Session s) ->
                s.createQuery("from Outlet o order by o.id asc", Outlet.class)
                        .setMaxResults(1).uniqueResult());
        if (first != null) {
            st.setActiveOutletId(first.getId());
        }
        return first;
    }

    public List<Outlet> listAll() {
        return Tx.tx((Session s) ->
                s.createQuery("from Outlet o order by o.id asc", Outlet.class).getResultList()
        );
    }

    public Outlet save(Outlet o) {
        return Tx.tx((Session s) -> {
            if (o.getId() == null) { s.persist(o); return o; }
            else return s.merge(o);
        });
    }

    public void delete(Outlet o) {
        Tx.tx((Session s) -> {
            Outlet managed = s.find(Outlet.class, o.getId());
            if (managed != null) s.remove(managed);
            return null;
        });
    }
}
