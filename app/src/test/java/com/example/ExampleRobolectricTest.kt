package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Customer
import com.example.data.model.Purchase
import com.example.data.model.User
import com.example.data.repository.CustomerRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mobile Shop Customer Manager", appName)
    }

    @Test
    fun `warranty expiry detection works correctly`() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // Purchase expiring in 15 days
        val purchaseExpiringSoon = Purchase(
            purchaseId = "p1",
            customerId = "c1",
            brand = "Samsung",
            model = "Galaxy S24",
            imei = "123456789012345",
            price = 79999.0,
            purchaseDate = now - (350 * oneDay),
            warrantyExpiry = now + (15 * oneDay)
        )

        assertTrue(purchaseExpiringSoon.isWarrantyActive)
        assertTrue(purchaseExpiringSoon.isExpiringIn30Days)
        assertEquals(15L, purchaseExpiringSoon.daysUntilWarrantyExpires)

        // Expired purchase
        val expiredPurchase = Purchase(
            purchaseId = "p2",
            customerId = "c1",
            brand = "Apple",
            model = "iPhone 13",
            imei = "987654321098765",
            price = 59999.0,
            purchaseDate = now - (500 * oneDay),
            warrantyExpiry = now - (10 * oneDay)
        )

        assertFalse(expiredPurchase.isWarrantyActive)
        assertFalse(expiredPurchase.isExpiringIn30Days)
    }

    @Test
    fun `dashboard summary calculates correctly`() {
        val now = System.currentTimeMillis()
        val customers = listOf(
            Customer(customerId = "c1", name = "Indrajit", mobile = "9876543210", createdAt = now),
            Customer(customerId = "c2", name = "Rahul", mobile = "9825012345", createdAt = now - (48 * 60 * 60 * 1000))
        )
        val purchases = listOf(
            Purchase(purchaseId = "p1", customerId = "c1", price = 50000.0, warrantyExpiry = now + (10L * 24 * 60 * 60 * 1000)),
            Purchase(purchaseId = "p2", customerId = "c2", price = 30000.0, warrantyExpiry = now + (100L * 24 * 60 * 60 * 1000))
        )

        val repo = CustomerRepository()
        val summary = repo.calculateSummary(customers, purchases)

        assertEquals(2, summary.totalCustomers)
        assertEquals(1, summary.registeredToday)
        assertEquals(1, summary.warrantyExpiringSoon)
        assertEquals(80000.0, summary.totalSalesValue, 0.01)
    }

    @Test
    fun `user role permissions correctly distinguish admin and staff`() {
        val admin = User(uid = "u1", name = "Owner", email = "admin@shop.com", role = User.ROLE_ADMIN)
        val staff = User(uid = "u2", name = "Staff", email = "staff@shop.com", role = User.ROLE_STAFF)

        assertTrue(admin.isAdmin)
        assertFalse(staff.isAdmin)
    }

    @Test
    fun `credentials authenticate owner and staff correctly`() = kotlinx.coroutines.test.runTest {
        val authRepo = com.example.data.repository.AuthRepository()

        // Owner credentials
        val ownerResult = authRepo.signIn("indrajit3232@gmail.com", "mobileshop4578")
        assertTrue(ownerResult.isSuccess)
        val owner = ownerResult.getOrNull()
        assertNotNull(owner)
        assertEquals("indrajit3232@gmail.com", owner?.email)
        assertTrue(owner?.isAdmin == true)

        // Staff credentials
        val staffResult = authRepo.signIn("staff3232@gmail.com", "mobileshop4578")
        assertTrue(staffResult.isSuccess)
        val staff = staffResult.getOrNull()
        assertNotNull(staff)
        assertEquals("staff3232@gmail.com", staff?.email)
        assertFalse(staff?.isAdmin == true)
    }
}
