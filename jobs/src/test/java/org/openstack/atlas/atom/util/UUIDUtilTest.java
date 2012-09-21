package org.openstack.atlas.atom.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RunWith(Enclosed.class)
public class UUIDUtilTest {
    public static class WhenGeneratingHashes {

        String baseUUID;


        @Before
        public void standUp() {
            //UUID=(Region, resourceID, tenantID)
            baseUUID = "DFW_123_23456";
        }

        @Test
        public void shouldGenerateMD5Hash() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDMD5Hash(baseUUID);
            Assert.assertNotNull(uuid);
        }

        @Test
        public void shouldGenerateSHA256Hash() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDSHA256(baseUUID);
            Assert.assertNotNull(uuid);
        }

        @Test
        public void shouldNotCreateSameSHA256HashForDifferentString() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDSHA256(baseUUID);
            UUID uuid2 = UUIDUtil.genUUIDSHA256("ORD_123_23456");
            Assert.assertNotSame(uuid, uuid2);
        }

        @Test
        public void shouldNotCreateSameMD5HashForDifferentString() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDMD5Hash(baseUUID);
            UUID uuid2 = UUIDUtil.genUUIDMD5Hash("ORD_123_23456");
            Assert.assertNotSame(uuid, uuid2);
        }

        @Test
        public void shouldCreateSameSHA256HashForDifferentString() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDSHA256(baseUUID);
            UUID uuid2 = UUIDUtil.genUUIDSHA256(baseUUID);
            Assert.assertEquals(uuid, uuid2);
        }

        @Test
        public void shouldCreateSameMD5HashForDifferentString() throws NoSuchAlgorithmException {
            UUID uuid = UUIDUtil.genUUIDMD5Hash(baseUUID);
            UUID uuid2 = UUIDUtil.genUUIDMD5Hash(baseUUID);
            Assert.assertEquals(uuid, uuid2);
        }
    }
}
