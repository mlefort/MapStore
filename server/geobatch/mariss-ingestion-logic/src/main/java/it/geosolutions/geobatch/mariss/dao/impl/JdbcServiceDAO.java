/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.geobatch.mariss.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import it.geosolutions.geobatch.mariss.dao.ServiceDAO;
import it.geosolutions.geobatch.mariss.model.AreaOfInterest;
import it.geosolutions.geobatch.mariss.model.Product;
import it.geosolutions.geobatch.mariss.model.Sensor;
import it.geosolutions.geobatch.mariss.model.SensorMode;
import it.geosolutions.geobatch.mariss.model.Service;

/**
 * <pre>
 * {@code
 *  CREATE TYPE service_status AS ENUM ('NEW', 'AOI', 'ACQUISITIONLIST', 'ACQUISITIONPLAN', 'PRODUCTS', 'INGESTED');
 *  CREATE TABLE service
 *    (
 *      "ID" serial NOT NULL,
 *      "SERVICE_ID" text NOT NULL,
 *      "PARENT" text NOT NULL,
 *      "USER" varchar(80) NOT NULL,
 *      "STATUS" character varying(80) NOT NULL DEFAULT 'NEW',
 *      CONSTRAINT service_pkey PRIMARY KEY ("ID")
 *    )
 *    WITH (
 *      OIDS=FALSE
 *    );
 *    ALTER TABLE service
 *      OWNER TO mariss;
 * </pre>
 * 
 * @author Alessio
 * 
 */
public class JdbcServiceDAO implements ServiceDAO {

    private DataSource dataSource;

    @Override
    public Service findByServiceId(String serviceId) {

        String sql = "SELECT * FROM SERVICE WHERE \"SERVICE_ID\" = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, serviceId);
            Service service = null;
            rs = ps.executeQuery();
            if (rs.next()) {
                service = new Service(rs.getString("SERVICE_ID"), rs.getString("PARENT"),
                        rs.getString("USER"), rs.getString("STATUS"));
                service.setId(rs.getInt("ID"));
                if (rs.getString("acq_plan_url") != null) {
                    service.setAcqPlanUrl(rs.getString("acq_plan_url"));
                }
            } else {
                return null;
            }

            // Retrieve the AOI
            sql = "SELECT fid, \"desc\", service_name, st_asewkt(the_geom) as thegeom, start, \"end\", status FROM AOIS WHERE service_name = ?";

            ps = conn.prepareStatement(sql);
            String userId = service.getUser();
            ps.setString(1, serviceId);
            AreaOfInterest aoi = null;
            rs = ps.executeQuery();
            if (rs.next()) {
                aoi = new AreaOfInterest(serviceId, rs.getString("desc"), rs.getString("thegeom"),
                        rs.getDate("start"), rs.getDate("end"), rs.getString("status"));
                aoi.setId(rs.getInt("fid"));
                service.setAoi(aoi);
            }
            rs.close();
            ps.close();

            // Retrieve the SENSORS
            sql = "SELECT id, sensor_type, sensor_mode, service_id FROM sensor WHERE service_id = ?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, service.getServiceId());
            List<Sensor> sensors = new ArrayList<Sensor>();
            rs = ps.executeQuery();
            while (rs.next()) {
                Sensor sensor = new Sensor(rs.getString("sensor_type"),
                        new SensorMode(rs.getString("sensor_mode")));
                sensor.setId(rs.getInt("id"));
                sensors.add(sensor);
            }

            service.setSensors(sensors);

            rs.close();
            ps.close();

            // Retrieve the PRODUCTS
            sql = "SELECT servicename, identifier, st_asewkt(bbox) as bbox, \"time\", variable, sartype, outfilelocation, originalfilepath, layername, partition, numoilspill, numshipdetect FROM ingestionproducts WHERE servicename = ?";

            ps = conn.prepareStatement(sql);
            ps.setString(1, service.getServiceId());
            List<Product> products = new ArrayList<Product>();
            rs = ps.executeQuery();
            while (rs.next()) {
                Product prod = new Product(rs.getString("identifier"), rs.getString("bbox"),
                        rs.getDate("time"), rs.getString("variable"), rs.getString("sartype"),
                        rs.getString("outfilelocation"), rs.getString("originalfilepath"),
                        rs.getString("layername"), rs.getString("partition"),
                        rs.getInt("numoilspill"), rs.getInt("numshipdetect"));
                products.add(prod);
            }

            service.setProducts(products);

            rs.close();
            ps.close();

            return service;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }

    @Override
    public List<Service> findByUser(String userId) {

        String sql = (userId != null ? "SELECT * FROM SERVICE WHERE \"USER\" = ?" : "SELECT * FROM SERVICE");

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            if (userId != null) {
                ps.setString(1, userId);
            }
            List<Service> services = new ArrayList<Service>();
            ResultSet rs = ps.executeQuery();
            Service service;
            while (rs.next()) {
                service = new Service(rs.getString("SERVICE_ID"), rs.getString("PARENT"),
                        rs.getString("USER"), rs.getString("STATUS"));
                
                service.setId(rs.getInt("ID"));
                if (rs.getString("acq_plan_url") != null) {
                    service.setAcqPlanUrl(rs.getString("acq_plan_url"));
                }
                
                services.add(service);
            }
            rs.close();
            ps.close();

            for (Service ss : services) {
                ss = getServiceDetails(conn, ss);
            }

            return services;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }

    @Override
    public List<Service> getServiceAccessByUser(String userId) {

        String sql = "SELECT SERVICE.* FROM SERVICE JOIN SERVICE_ACCESS ON SERVICE.\"SERVICE_ID\" = SERVICE_ACCESS.\"SERVICE_ID\" WHERE SERVICE_ACCESS.\"USER\" = ?";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            List<Service> services = new ArrayList<Service>();
            ResultSet rs = ps.executeQuery();
            Service service;
            while (rs.next()) {
                service = new Service(rs.getString("SERVICE_ID"), rs.getString("PARENT"),
                        rs.getString("USER"), rs.getString("STATUS"));
                
                service.setId(rs.getInt("ID"));
                if (rs.getString("acq_plan_url") != null) {
                    service.setAcqPlanUrl(rs.getString("acq_plan_url"));
                }
                services.add(service);
            }
            rs.close();
            ps.close();

            for (Service ss : services) {
                ss = getServiceDetails(conn, ss);
            }

            return services;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }
    
    /**
     * @param conn
     * @param ss
     * @return 
     * @throws SQLException
     */
    protected Service getServiceDetails(Connection conn, Service ss) throws SQLException {
        String sql;
        PreparedStatement ps;
        ResultSet rs;
        // String serviceId = userId + "@" + ss.getServiceId();

        // Retrieve the AOI
        sql = "SELECT fid, \"desc\", service_name, st_asewkt(the_geom) as thegeom, start, \"end\", status FROM AOIS WHERE service_name = ?";

        ps = conn.prepareStatement(sql);
        ps.setString(1, ss.getServiceId());
        AreaOfInterest aoi = null;
        rs = ps.executeQuery();
        if (rs.next()) {
            aoi = new AreaOfInterest(ss.getServiceId(), rs.getString("desc"),
                    rs.getString("thegeom"), rs.getDate("start"), rs.getDate("end"),
                    rs.getString("status"));
            aoi.setId(rs.getInt("fid"));
            ss.setAoi(aoi);
        }
        rs.close();
        ps.close();

        // Retrieve the SENSORS
        sql = "SELECT id, sensor_type, sensor_mode, service_id FROM sensor WHERE service_id = ?";

        ps = conn.prepareStatement(sql);
        ps.setString(1, ss.getServiceId());
        List<Sensor> sensors = new ArrayList<Sensor>();
        rs = ps.executeQuery();
        while (rs.next()) {
            Sensor sensor = new Sensor(rs.getString("sensor_type"),
                    new SensorMode(rs.getString("sensor_mode")));
            sensor.setId(rs.getInt("id"));
            sensors.add(sensor);
        }
        rs.close();
        ps.close();

        ss.setSensors(sensors);

        // Retrieve the PRODUCTS
        sql = "SELECT servicename, identifier, st_asewkt(bbox) as bbox, \"time\", variable, sartype, outfilelocation, originalfilepath, layername, partition, numoilspill, numshipdetect FROM ingestionproducts WHERE servicename = ?";

        ps = conn.prepareStatement(sql);
        ps.setString(1, ss.getServiceId());
        List<Product> products = new ArrayList<Product>();
        rs = ps.executeQuery();
        while (rs.next()) {
            Product prod = new Product(rs.getString("identifier"), rs.getString("bbox"),
                    rs.getDate("time"), rs.getString("variable"), rs.getString("sartype"),
                    rs.getString("outfilelocation"), rs.getString("originalfilepath"),
                    rs.getString("layername"), rs.getString("partition"),
                    rs.getInt("numoilspill"), rs.getInt("numshipdetect"));
            products.add(prod);
        }

        ss.setProducts(products);

        rs.close();
        ps.close();
        
        return ss;
    }

    @Override
    public List<SensorMode> getSensorModes() {

        String sql = "SELECT * FROM sensor_modes";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            List<SensorMode> sensorModes = new ArrayList<SensorMode>();
            ResultSet rs = ps.executeQuery();
            SensorMode sensorMode;
            while (rs.next()) {
                sensorMode = new SensorMode(rs.getString("sensor_mode"));
                sensorMode.setId(rs.getInt("id"));

                sensorModes.add(sensorMode);
            }
            rs.close();
            ps.close();

            return sensorModes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }

    @Override
    public List<Sensor> getSensors() {

        String sql = "SELECT * FROM sensors";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            List<Sensor> sensors = new ArrayList<Sensor>();
            ResultSet rs = ps.executeQuery();
            Sensor sensor;
            while (rs.next()) {
                sensor = new Sensor(rs.getString("sensor"), null);
                sensor.setId(rs.getInt("id"));

                sensors.add(sensor);
            }
            rs.close();
            ps.close();

            return sensors;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }

    @Override
    public boolean insert(Service service) {

        boolean result = false;

        String sql = "INSERT INTO SERVICE (\"SERVICE_ID\", \"PARENT\", \"USER\", \"STATUS\") VALUES (?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, service.getServiceId());
            ps.setString(2, service.getParent());
            ps.setString(3, service.getUser());
            ps.setString(4, service.getStatus());
            result = ps.executeUpdate() > 0;
            ps.close();

            if (result) {
                sql = "INSERT INTO SERVICE_ACCESS (\"SERVICE_ID\", \"USER\") VALUES (?, ?)";
                ps = conn.prepareStatement(sql);
                ps.setString(1, service.getServiceId());
                ps.setString(2, service.getUser());
                result = ps.executeUpdate() > 0;
                ps.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;

    }

    @Override
    public boolean insertOrUpdate(AreaOfInterest aoi) {

        boolean result = false;

        String sql = "SELECT * FROM AOIS WHERE service_name = ?";
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, aoi.getServiceId());
            ResultSet rs = ps.executeQuery();
            boolean performUpdate = rs.next();
            rs.close();

            if (performUpdate) {
                // perform update
                sql = "UPDATE aois SET the_geom=?, start=?, \"end\"=?, status=?, \"desc\"=? WHERE service_name = ?";
                if (!ps.isClosed())
                    ps.close();
                ps = conn.prepareStatement(sql);
                ps.setString(1, aoi.getTheGeom());
                ps.setDate(2, aoi.getStartTime());
                ps.setDate(3, aoi.getEndTime());
                ps.setString(4, aoi.getStatus());
                ps.setString(5, aoi.getDescription());
                ps.setString(6, aoi.getServiceId());
                result = ps.execute() && ps.getUpdateCount() > 0;
            } else {
                // perform insert
                sql = "INSERT INTO aois(service_name, the_geom, start, \"end\", status, \"desc\") VALUES (?, ?, ?, ?, ?, ?)";
                if (!ps.isClosed())
                    ps.close();
                ps = conn.prepareStatement(sql);
                ps.setString(1, aoi.getServiceId());
                ps.setString(2, aoi.getTheGeom());
                ps.setDate(3, aoi.getStartTime());
                ps.setDate(4, aoi.getEndTime());
                ps.setString(5, aoi.getStatus());
                ps.setString(6, aoi.getDescription());
                result = ps.executeUpdate() > 0;
            }
            ps.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;

    }

    @Override
    public boolean insertOrUpdate(String serviceId, List<Sensor> sensors) {

        boolean result = false;

        String sql = "DELETE FROM sensor WHERE service_id = ?";
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceId);
            ps.executeUpdate();
            ps.close();

            sql = "INSERT INTO sensor(sensor_type, sensor_mode, service_id) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql);

            for (Sensor sensor : sensors) {
                ps.setString(1, sensor.getSensor());
                ps.setString(2, sensor.getSensorMode().getSensorMode());
                ps.setString(3, serviceId);
                ps.addBatch();
            }
            result = ps.executeBatch().length > 0;
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;

    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean updateServiceStatus(Service service, String status) {
        boolean result = false;

        String sql = "UPDATE SERVICE SET \"STATUS\" = ? WHERE \"USER\" = ? AND \"SERVICE_ID\" = ?";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, service.getUser());
            ps.setString(3, service.getServiceId());
            result = ps.execute() && ps.getUpdateCount() > 0;
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;
    }

    @Override
    public boolean updateServiceAcqPlan(Service service, String acqPlanUrl) {
        boolean result = false;

        String sql = "UPDATE SERVICE SET \"STATUS\" = ?, acq_plan_url = ? WHERE \"USER\" = ? AND \"SERVICE_ID\" = ?";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "ACQUISITIONPLAN");
            ps.setString(2, acqPlanUrl);
            ps.setString(3, service.getUser());
            ps.setString(4, service.getServiceId());
            result = ps.execute() && ps.getUpdateCount() > 0;
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;
    }
    
    @Override
    public boolean updateServicesAccess(String userId, List<String> serviceIds) {
        boolean result = false;

        String sql = "DELETE FROM SERVICE_ACCESS WHERE \"USER\" = ?";

        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.executeUpdate();
            ps.close();

            sql = "INSERT INTO SERVICE_ACCESS(\"USER\", \"SERVICE_ID\") VALUES (?, ?)";
            ps = conn.prepareStatement(sql);

            for (String serviceId : serviceIds) {
                ps.setString(1, userId);
                ps.setString(2, serviceId);
                ps.addBatch();
            }
            result = ps.executeBatch().length > 0;            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        return result;
    }
    
    @Override
    public boolean delete(String serviceId, Map<String, String> serviceAuxiliaryTables) {
        boolean result = false;

        final String sql_template = "DELETE FROM {{table_name}} WHERE {{key_field}} = ?";

        Connection conn = null;

        if (serviceAuxiliaryTables != null && !serviceAuxiliaryTables.isEmpty()) {
            for (Entry<String, String> entry : serviceAuxiliaryTables.entrySet()) {

                String sql = sql_template.replace("{{table_name}}", entry.getKey())
                        .replace("{{key_field}}", entry.getValue());

                try {
                    conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, serviceId);
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    // Skip delete
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                        }
                    }
                }
            }
        }

        String sql = sql_template.replace("{{table_name}}", "SERVICE_ACCESS").replace("{{key_field}}",
                "\"SERVICE_ID\"");

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

        sql = sql_template.replace("{{table_name}}", "SERVICE").replace("{{key_field}}",
                "\"SERVICE_ID\"");

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, serviceId);
            result = ps.executeUpdate() > 0;
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        
        return result;
    }

}
